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

"""

from django.conf import settings
from django.conf.urls.static import static
from django.contrib import admin
from django.urls import path, include
from rest_auth.views import PasswordResetView, PasswordResetConfirmView, \
    PasswordChangeView
from rest_framework import routers
from rest_framework_nested.routers import NestedSimpleRouter

from datasources.views import DataSourceViewSet
from registration.views import UserRegistrationView, UserConfirmationView
from wallet import views as wallet_views
from wallet.views import WalletAPIRootView, LoginView, LogoutView
from apikeys.views import ApikeyViewSet
from jobs.views import JobViewSet, TaskViewSet, TaskErrorViewSet
from projects.views import ProjectViewSet, ProjectUserViewSet
from permissions.views import PermissionViewSet

router = routers.DefaultRouter()
router.APIRootView = WalletAPIRootView
router.register('users', wallet_views.UserViewSet, basename='user')
router.register('groups', wallet_views.GroupViewSet, basename='group')
router.register('projects', ProjectViewSet, basename='project')

projects_router = NestedSimpleRouter(router, 'projects', lookup='project')
projects_router.register('jobs', JobViewSet, basename='job')
projects_router.register('tasks', TaskViewSet, basename='task')
projects_router.register('taskerrors', TaskErrorViewSet, basename='taskerror')
projects_router.register('users', ProjectUserViewSet, basename='projectuser')
projects_router.register('apikeys', ApikeyViewSet, basename='apikey')
projects_router.register('permissions', PermissionViewSet, basename='permission')
projects_router.register('datasources', DataSourceViewSet, basename='datasource')


# Use this variable to add standalone views to the urlspatterns and have them accessible
# from the root DRF browsable API. The tuples are in the form (LABEL, path()).
BROWSABLE_API_URLS = [
    ('password-change', path('api/v1/password/change/', PasswordChangeView.as_view(),
                             name='api-password-change')),
    ('password-reset', path('api/v1/password/reset/', PasswordResetView.as_view(),
                            name='api-password-reset')),
    ('password-reset-confirmation', path('api/v1/password/reset/confirm/',
                                         PasswordResetConfirmView.as_view(),
                                         name='api-password-reset-confirm')),
    ('logout', path('api/v1/logout/', LogoutView.as_view(), name='api-logout')),
    ('user-registration', path('api/v1/accounts/register',
                               UserRegistrationView.as_view(),
                               name='api-user-register')),
    ('user-registration-confirmation', path('api/v1/accounts/confirm',
                                            UserConfirmationView.as_view(),
                                            name='api-user-confirm'))
]


urlpatterns = [
    path('admin/', admin.site.urls),
    path('api/v1/login/', LoginView.as_view(), name='api-login'),
    path('api/v1/', include(router.urls)),
    path('api/v1/', include(projects_router.urls)),
    path('api/v1/health/', include('health_check.urls'))
]
urlpatterns += [i[1] for i in BROWSABLE_API_URLS]
urlpatterns += static(settings.STATIC_URL, document_root=settings.STATIC_ROOT)
