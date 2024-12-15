from django.shortcuts import render
from django.utils import timezone
from django.shortcuts import render, get_object_or_404, redirect
from django.contrib.auth.decorators import login_required

from .models import Post
from .forms import PostForm

from rest_framework import viewsets, permissions, authentication
from rest_framework.response import Response
from .serializers import PostSerializer

from rest_framework.decorators import action

from rest_framework.request import Request
from rest_framework.parsers import JSONParser
import datetime
import pytz

def post_list(request):
    if request.user.is_authenticated:
        posts = Post.objects.filter(
            author=request.user,
            published_date__lte=timezone.now(),
        ).order_by('published_date')
        return render(request, 'blog/post_list.html', {'posts': posts})
    return render(request, 'blog/post_list.html', {'posts': Post.objects.none()})


def post_detail(request, pk):
    post = get_object_or_404(Post, pk=pk)
    return render(request, 'blog/post_detail.html', {'post': post})

@login_required
def post_new(request):
    if request.method == 'POST':
        form = PostForm(request.POST, files=request.FILES)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = request.user
            post.published_date = timezone.now()
            post.save()
            return redirect('post_detail', pk=post.pk)
    else:
        form = PostForm()
    return render(request, 'blog/post_edit.html', {'form': form})


@login_required
def post_edit(request, pk):
    post = get_object_or_404(Post, pk=pk)
    if request.user != post.author:
        return redirect('post_detail', pk=post.pk)
    if request.method == "POST":
        form = PostForm(request.POST, instance=post, files=request.FILES)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = request.user
            post.published_date = timezone.now()
            post.save()
        return redirect('post_detail', pk=post.pk)
    else:
        form = PostForm(instance=post)
    return render(request, 'blog/post_edit.html', {'form': form})


class blogImage(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer
    authentication_classes = [authentication.SessionAuthentication, authentication.TokenAuthentication]
    permission_classes = [permissions.IsAuthenticated]
    
    http_method_names = ['post']

    def perform_create(self, serializer):
        serializer.save(author=self.request.user)

class imagePostSearch(viewsets.ViewSet):
    authentication_classes = [authentication.SessionAuthentication, authentication.TokenAuthentication]
    permission_classes = [permissions.IsAuthenticated]
    parser_classes = [JSONParser]
    serializer_class = PostSerializer

    def list(self, request):
        return Response([])

    @action(detail=False, methods=['post'])
    def q(self, request: Request):
        is_all_period = False
        has_date_filter = False
        
        if 'isAllPeriod' in request.data:
            flag = request.data['isAllPeriod']
            if isinstance(flag, bool):
                if flag == True:
                    is_all_period = True

        elif 'hasDateFilter' in request.data:
            flag = request.data['hasDateFilter']
            if isinstance(flag, bool):
                if flag == True:
                    if 'year' in request.data and 'month' in request.data and 'day' in request.data:
                        year, month, day = request.data['year'], request.data['month'], request.data['day']
                        if isinstance(year, int) and isinstance(month, int) and isinstance(day, int):
                            if 2000 <= year <= 2025 and 1 <= month <= 12 and 1 <= day <= 31:
                                has_date_filter = True
                                
        if is_all_period:
            posts = Post.objects.filter(author=request.user)
        else:
            tz = pytz.timezone('Asia/Seoul')
            if has_date_filter:
                year, month, day = request.data['year'], request.data['month'], request.data['day']
                x = datetime.datetime(year, month, day, tzinfo=tz)
                posts = Post.objects.filter(author=request.user, published_date__range=(x, x + datetime.timedelta(days=1)))
            else: # is_all_period x has_date_filter x -> all_period
                now = datetime.datetime.now(tz=tz)
                x = datetime.datetime(now.year, now.month, now.day, tzinfo=tz)
                posts = Post.objects.filter(author=request.user, published_date__range=(x, x + datetime.timedelta(days=1)))
                
        if 'query' in request.data:
            if isinstance(request.data['query'], str):
                query = request.data['query']
                posts = posts & Post.objects.filter(text__icontains=query)
        
        serializer = PostSerializer(posts, many=True, context={'request': request})
        return Response(serializer.data)
