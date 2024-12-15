from blog.models import Post
from rest_framework import serializers
from django.contrib.auth.models import User


class PostSerializer(serializers.HyperlinkedModelSerializer):
    author = serializers.PrimaryKeyRelatedField(queryset=User.objects.all(),
                                                required=False)
    image = serializers.ImageField(use_url=True)  
    
    def get_image(self, obj):
        if obj.image:
            request = self.context.get('request')
            return request.build_absolute_uri(obj.image.url)
        return None

    class Meta:
        model = Post
        fields = ('author', 'title', 'text', 'created_date', 'published_date',
                  'image')
