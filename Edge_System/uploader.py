import requests

class PhotoUploader:
    # HOST = 'http://127.0.0.1:8000'
    def __init__(self, host: str, username: str, password: str):
        self.host: str = host
        response = requests.post(self.host + '/api-token-auth/', {
            'username': username,
            'password': password
        })
        try:
            response.raise_for_status()
        except requests.HTTPError as e:
            print(e)
            
        json_body = response.json()
        
        if 'token' in json_body:
            self.token = json_body['token']
        else:
            raise
    
    def upload(self, title: str, text: str, path: str) -> bool:
        headers = {
            'Authorization': f'Token {self.token}',
            'Accept': 'application/json',
        }
        
        data = {
            'title': title,
            'text': text,
        }
        
        with open(path, 'rb') as image:
            file = {
                'image': image
            }
            
            response = requests.post(self.host + '/api_root/Post/', data=data, files=file, headers=headers)

            try:
                response.raise_for_status()
            except requests.HTTPError as _:
                return False

        return True
            
    
    def image_list(self):
        headers = {
            'Authorization': f'Token {self.token}',
            'Content-Type': 'application/json',
        }
        
        response = requests.post(self.host + '/api_root/search/q/', data='{}', headers=headers)
        
        try:
            response.raise_for_status()
        except:
            return None
        return response.json()
        
if __name__ == '__main__':
    # photo_uploader = PhotoUploader('http://127.0.0.1:8000', 'admin', '1234')
    photo_uploader = PhotoUploader('https://thinking.pythonanywhere.com', 'alice', '7@7K^ail')

    # photo_uploader.upload('title', 'text', '/Users/user/uni_2024/mobile/yolo/test.jpg')
    # photo_uploader.upload('1', '2', '../image_sample/photo1.jpeg')
    l = photo_uploader.image_list()
    print(l)


        
        
        
