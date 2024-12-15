from ultralytics import YOLO  # type: ignore
import cv2
import numpy as np
import os
from datetime import datetime
from dataclasses import dataclass
from torch import Tensor
from typing import Dict, List, Tuple, Optional, Set, Union, NamedTuple, Protocol, DefaultDict
import uploader
from collections import defaultdict

class DetectedObject(NamedTuple):
    """감지된 객체 정보를 저장하는 클래스"""
    class_name: str
    confidence: float

class ObjectInfo(NamedTuple):
    """객체의 중심점과 바운딩 박스 정보"""
    center: Tuple[float, float]
    bbox: np.ndarray

class Box(Protocol):
    """YOLO 박스 정보를 위한 프로토콜"""
    cls: Tensor  # 클래스 인덱스
    conf: Tensor  # 신뢰도
    xyxy: List[Tensor]  # 바운딩 박스 좌표 [x1, y1, x2, y2]

@dataclass
class Boxes:
    """YOLO 박스들의 컬렉션"""
    boxes: List[Box]
    
    def __getitem__(self, idx: int) -> Box:
        return self.boxes[idx]

@dataclass
class YOLOResults:
    """YOLO 모델의 예측 결과"""
    boxes: Boxes

    def __getitem__(self, idx: int) -> Boxes:
        return self.boxes

class ChangeDetector:
    def __init__(self, save_dir: str = 'detected_objects', 
                 distance_threshold: float = 30.0, 
                 conf_threshold: float = 0.25,
                 gap: int = 1,
                 debug=False) -> None:
        """
        초기화
        :param save_dir: 이미지 저장 디렉토리
        :param distance_threshold: 객체 이동 감지 거리 임계값 (픽셀)
        :param conf_threshold: 객체 검출 신뢰도 임계값
        """
        
        import logging
        logging.getLogger("ultralytics").setLevel(logging.WARNING)
        
        self.model = YOLO('yolov5s.pt')
        self.save_dir = save_dir
        self.distance_threshold = distance_threshold
        self.conf_threshold = conf_threshold
        self.tracked_object_centers: Set[Tuple[int, Tuple[float, float]]] = set()  # (class_id, center) 튜플을 저장
        self.initialized: bool = False
        self.gap: int = gap

        
        # COCO 데이터셋 클래스 이름
        self.class_names: List[str] = [
            'person', 'bicycle', 'car', 'motorcycle', 'airplane', 'bus', 'train', 'truck', 'boat', 
            'traffic light', 'fire hydrant', 'stop sign', 'parking meter', 'bench', 'bird', 'cat', 'dog', 
            'horse', 'sheep', 'cow', 'elephant', 'bear', 'zebra', 'giraffe', 'backpack', 'umbrella', 
            'handbag', 'tie', 'suitcase', 'frisbee', 'skis', 'snowboard', 'sports ball', 'kite', 
            'baseball bat', 'baseball glove', 'skateboard', 'surfboard', 'tennis racket', 'bottle', 
            'wine glass', 'cup', 'fork', 'knife', 'spoon', 'bowl', 'banana', 'apple', 'sandwich', 
            'orange', 'broccoli', 'carrot', 'hot dog', 'pizza', 'donut', 'cake', 'chair', 'couch', 
            'potted plant', 'bed', 'dining table', 'toilet', 'tv', 'laptop', 'mouse', 'remote', 
            'keyboard', 'cell phone', 'microwave', 'oven', 'toaster', 'sink', 'refrigerator', 'book', 
            'clock', 'vase', 'scissors', 'teddy bear', 'hair drier', 'toothbrush'
        ]
        
        # 디렉토리 생성
        os.makedirs(os.path.join(save_dir, 'initial'), exist_ok=True)
        os.makedirs(os.path.join(save_dir, 'changes'), exist_ok=True)

    def get_center(self, box: np.ndarray) -> Tuple[float, float]:
        """박스의 중심점 계산"""
        x1, y1, x2, y2 = box
        return ((x1 + x2) / 2, (y1 + y2) / 2)

    def calculate_distance(self, point1: Tuple[float, float], 
                         point2: Tuple[float, float]) -> float:
        """두 점 사이의 거리 계산"""
        return float(np.sqrt((point1[0] - point2[0])**2 + (point1[1] - point2[1])**2))

    def process_frame(self, frame: np.ndarray) -> Tuple[YOLOResults, List[DetectedObject]]:
        """
        프레임 처리 및 객체 검출
        :param frame: 처리할 프레임
        :return: YOLO 결과와 검출된 객체 리스트
        """
        results = YOLOResults(self.model(frame, conf=self.conf_threshold)[0])
        detected_objects: List[DetectedObject] = []
        
        for box in results[0].boxes:
            class_id = int(box.cls[0])
            conf = float(box.conf[0])
            class_name = self.class_names[class_id]
            detected_objects.append(DetectedObject(class_name, conf))
        
        return results, detected_objects

    def draw_detection_boxes(self, frame: np.ndarray, 
                           results: YOLOResults,
                           color: Tuple[int, int, int] = (0, 255, 0)) -> np.ndarray:
        """
        검출된 객체 박스 그리기
        :param frame: 원본 프레임
        :param results: YOLO 검출 결과
        :param color: 박스 색상 (B, G, R)
        :return: 박스가 그려진 프레임
        """
        frame_with_boxes = frame.copy()
        
        for box in results[0].boxes:
            class_id = int(box.cls[0])
            bbox = box.xyxy[0].cpu().numpy()
            conf = float(box.conf[0])
            
            x1, y1, x2, y2 = map(int, bbox)
            class_name = self.class_names[class_id]
            
            cv2.rectangle(frame_with_boxes, (x1, y1), (x2, y2), color, 2)
            cv2.putText(frame_with_boxes, f"{class_name} {conf:.2f}", 
                      (x1, y1-10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 2)
        
        return frame_with_boxes

    def save_frame(self, frame: np.ndarray, state: str = 'initial') -> str:
        """
        프레임 저장
        :param frame: 저장할 프레임
        :param state: 저장 상태 ('initial' 또는 'changes')
        :return: 저장된 파일 경로
        """
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"{timestamp}_full_frame.jpg"
        save_path = os.path.join(self.save_dir, state, filename)
        cv2.imwrite(save_path, frame)
        return save_path

    def process_detection_results(self, results: YOLOResults) -> Dict[int, List[ObjectInfo]]:
        """
        검출 결과를 객체 정보로 변환
        :param results: YOLO 검출 결과
        :return: 클래스별 객체 정보 딕셔너리
        """
        current_objects: DefaultDict[int, List[ObjectInfo]] = defaultdict(list)
        
        for box in results[0].boxes:
            class_id = int(box.cls[0])
            bbox = box.xyxy[0].cpu().numpy()
            center = self.get_center(bbox)
            
            current_objects[class_id].append(ObjectInfo(center=center, bbox=bbox))
            
        return current_objects
 
    def capture_initial_state(self, callback) -> None:
        """초기 상태 캡처"""
        print("초기 상태를 캡처합니다. 스페이스바를 누르면 캡처됩니다.")
        
        cap = cv2.VideoCapture(0)
        
        while True:
            ret, frame = cap.read()
            if not ret:
                break
            
            results, detected_objects = self.process_frame(frame)
            preview_frame = self.draw_detection_boxes(frame, results)
            
            cv2.imshow('Initial Capture (Press SPACE)', preview_frame)
            
            key = cv2.waitKey(1) & 0xFF
            if key == ord(' '):  # 스페이스바
                save_path = self.save_frame(preview_frame, 'initial')
                print(f"초기 상태가 저장되었습니다: {save_path}")
                
                self.initialized = True
                
                # 초기 객체들의 중심점 저장
                for class_id, objects in self.process_detection_results(results).items():
                    for obj in objects:
                        self.tracked_object_centers.add((class_id, obj.center))
                        
                class_names: List[Tuple[str, float]] = []
                print("\n감지된 객체들:")
                for obj in detected_objects:
                    class_names.append((obj.class_name, obj.confidence))
                    print(f"- {obj.class_name} (신뢰도: {obj.confidence:.2f})")
                print(f"\n총 {len(detected_objects)}개의 객체가 감지되었습니다.")
                callback(save_path, class_names)
                break
            elif key == ord('q'):
                break
                
        cap.release()
        cv2.destroyAllWindows()

    def find_closest_tracked_object(self, class_id: int, center: Tuple[float, float]) -> Optional[Tuple[float, float]]:
        """
        현재 객체와 가장 가까운 추적 중인 객체 찾기
        :return: 가장 가까운 추적 객체의 중심점 또는 None
        """
        min_distance = float('inf')
        closest_center = None
        
        for tracked_class_id, tracked_center in self.tracked_object_centers:
            if tracked_class_id == class_id:
                distance = self.calculate_distance(center, tracked_center)
                if distance < min_distance:
                    min_distance = distance
                    closest_center = tracked_center
        
        return closest_center
    
    def monitor_changes(self, change_callback) -> None:
        """변화 모니터링"""
        
        if not self.initialized:
            print("먼저 초기 상태를 캡처해주세요.")
            return
            
        cap = cv2.VideoCapture(0)
        
        accident: bool = False
        
        while True:
            ret, frame = cap.read()
            if not ret:
                break
            
            results, _ = self.process_frame(frame)
            current_frame = self.draw_detection_boxes(frame, results, color=(255, 255, 255))
            current_objects = self.process_detection_results(results)
            
            
            # 추적 중인 객체들의 위치 변화만 확인
            for class_id, curr_objects in current_objects.items():
                for curr_obj in curr_objects:
                    closest_tracked_center = self.find_closest_tracked_object(class_id, curr_obj.center)
                    
                    if closest_tracked_center is not None:
                        # 추적 중인 객체의 위치 변화 확인
                        distance = self.calculate_distance(curr_obj.center, closest_tracked_center)
                        if distance > self.distance_threshold:
                            save_path = self.save_frame(current_frame, 'changes')
                            print('accident occured')
                            print(f"{self.class_names[class_id]}: 위치 변화 감지 ({distance:.1f}px)")
                            change_callback(save_path, self.class_names[class_id])
                            accident = True
            
            if accident:
                break
                            
            cv2.imshow('Object Detection & Change Monitoring', current_frame)
            
            if cv2.waitKey(1) & 0xFF == ord('q'):
                break
                
        cap.release()
        cv2.destroyAllWindows()
        


# 사용 예시
if __name__ == "__main__":
    detector = ChangeDetector(
        distance_threshold=50.0,
        conf_threshold=0.3
    )
    
    _uploader = uploader.PhotoUploader(host='http://127.0.0.1:8000', username='admin', password='1234')

    def upload_init(image_path: str, names: List[Tuple[str, float]]):
        print(image_path, names)
        highest_confidence = max(names, key=lambda x: x[1])[0]
        text = ', '.join(map(lambda x: x[0], names))
        _uploader.upload(title=highest_confidence, text=text, path=image_path)
    
    def upload_change(image_path: str, class_name: str):
        _uploader.upload(title=class_name, text=class_name, path=image_path)

    # 1단계: 초기 상태 캡처
    detector.capture_initial_state(upload_init)
    
    # 2단계: 변화 모니터링 시작
    detector.monitor_changes(upload_change)