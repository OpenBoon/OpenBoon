"""wallet URL Configuration

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/2.1/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))

NOTE:
    We are currently using session authentication. If we ever decide to we need to use
    JWT auth again add the following paths.

    path('auth/token/', TokenObtainPairView.as_view(), name='token_obtain_pair'),
    path('auth/refresh/', TokenRefreshView.as_view(), name='token_refresh'),

"""
from django.conf import settings
from django.conf.urls.static import static
from django.contrib import admin
from django.urls import path, include, re_path
from django.views.decorators.csrf import ensure_csrf_cookie
from rest_framework import routers
from rest_framework_nested.routers import NestedSimpleRouter
from rest_framework_simplejwt.views import (TokenObtainPairView, TokenRefreshView)

from jobs.views import JobsViewSet
from projects.views import ProjectViewSet
from wallet import views as wallet_views

router = routers.DefaultRouter()
router.register('users', wallet_views.UserViewSet, basename='user')
router.register('groups', wallet_views.GroupViewSet, basename='group')
router.register('projects', ProjectViewSet, basename='project')

projects_router = NestedSimpleRouter(router, 'projects', lookup='project')
projects_router.register('jobs', JobsViewSet, basename='job')

urlpatterns = [
    path('admin/', admin.site.urls),
    path('api/v1/login/', wallet_views.LoginView.as_view(), name='api-login'),
    path('api/v1/logout/', wallet_views.LogoutView.as_view(), name='api-logout'),
    path('api/v1/', include(router.urls)),
    path('api/v1/', include(projects_router.urls)),
    path('health/', include('health_check.urls')),
    re_path('', ensure_csrf_cookie(wallet_views.FrontendAppView.as_view()))
] + static(settings.STATIC_URL, document_root=settings.STATIC_ROOT)
