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
from django.contrib.auth import get_user_model
from django.urls import path, include
from rest_framework import routers
from rest_framework_nested.routers import NestedSimpleRouter

from agreements.views import AgreementViewSet
from apikeys.views import ApikeyViewSet
from assets.views import (AssetViewSet, FileCategoryViewSet,
                          FileNameViewSet, WebVttViewSet)
from datasources.views import DataSourceViewSet
from faces.views import FaceViewSet
from gcpmarketplace.views import signup_success, SignUpView
from jobs.views import JobViewSet, TaskViewSet, TaskErrorViewSet, JobTaskViewSet
from models.views import ModelViewSet
from modules.views import ModuleViewSet, ProviderViewSet
from permissions.views import PermissionViewSet
from projects.views import ProjectViewSet, ProjectUserViewSet
from registration.admin import UserAdmin
from registration.views import UserRegistrationView, UserConfirmationView, \
    ApiPasswordChangeView, LogoutView, MeView, LoginView, ApiPasswordResetView, \
    ApiPasswordResetConfirmView
from roles.views import RolesViewSet
from searches.views import SearchViewSet
from supportadmin.admin import support_admin_site
from visualizations.views import VisualizationViewSet
from wallet.views import WalletAPIRootView

router = routers.DefaultRouter()
router.APIRootView = WalletAPIRootView
router.register('projects', ProjectViewSet, basename='project')
router.register('me/agreements', AgreementViewSet, basename='agreement')

projects_router = NestedSimpleRouter(router, 'projects', lookup='project')
projects_router.register('jobs', JobViewSet, basename='job')
projects_router.register('tasks', TaskViewSet, basename='task')
projects_router.register('task_errors', TaskErrorViewSet, basename='taskerror')
projects_router.register('users', ProjectUserViewSet, basename='projectuser')
projects_router.register('assets', AssetViewSet, basename='asset')
projects_router.register('api_keys', ApikeyViewSet, basename='apikey')
projects_router.register('roles', RolesViewSet, basename='role')
projects_router.register('permissions', PermissionViewSet, basename='permission')
projects_router.register('data_sources', DataSourceViewSet, basename='datasource')
projects_router.register('modules', ModuleViewSet, basename='module')
projects_router.register('providers', ProviderViewSet, basename='provider')
projects_router.register('searches', SearchViewSet, basename='search')
projects_router.register('faces', FaceViewSet, basename='face')
projects_router.register('visualizations', VisualizationViewSet, basename='visualization')
projects_router.register('models', ModelViewSet, basename='model')


assets_files_router = NestedSimpleRouter(projects_router, 'assets', lookup='asset')
assets_files_router.register('files/category', FileCategoryViewSet, basename='category')
assets_files_router.register('webvtt', WebVttViewSet, basename='webvtt')

assets_file_names_router = NestedSimpleRouter(assets_files_router, 'files/category', lookup='category')  # noqa
assets_file_names_router.register('name', FileNameViewSet, basename='file_name')

jobs_router = NestedSimpleRouter(projects_router, 'jobs', lookup='job')
jobs_router.register('tasks', JobTaskViewSet, basename='job-detail-task')

# Use this variable to add standalone views to the urlspatterns and have them accessible
# from the root DRF browsable API. The tuples are in the form (LABEL, path()).
BROWSABLE_API_URLS = [
    ('password-change', path('api/v1/password/change/', ApiPasswordChangeView.as_view(),
                             name='api-password-change')),
    ('password-reset', path('api/v1/password/reset/', ApiPasswordResetView.as_view(),
                            name='api-password-reset')),
    ('password-reset-confirmation', path('api/v1/password/reset/confirm/',
                                         ApiPasswordResetConfirmView.as_view(),
                                         name='api-password-reset-confirm')),
    ('logout', path('api/v1/logout/', LogoutView.as_view(), name='api-logout')),
    ('me', path('api/v1/me/', MeView.as_view(), name='me')),
    ('user-registration', path('api/v1/accounts/register',
                               UserRegistrationView.as_view(),
                               name='api-user-register')),
    ('user-registration-confirmation', path('api/v1/accounts/confirm',
                                            UserConfirmationView.as_view(),
                                            name='api-user-confirm'))
]

admin.site.enable_nav_sidebar = False
support_admin_site.enable_nav_sidebar = False

urlpatterns = [
    path('admin/', support_admin_site.urls),
    path('api/v1/login/', LoginView.as_view(), name='api-login'),
    path('api/v1/', include(router.urls)),
    path('api/v1/', include(projects_router.urls)),
    path('api/v1/', include(assets_files_router.urls)),
    path('api/v1/', include(assets_file_names_router.urls)),
    path('api/v1/', include(jobs_router.urls)),
    path('api/v1/health/', include('health_check.urls')),
    path('marketplace/signup/', SignUpView.as_view(), name='gcpmarketplace-signup'),
    path('marketplace/signup_success/', signup_success, name='gcpmarketplace-signup-success'),
]
if settings.SUPERADMIN:
    # Registering the user model here because of the order in which the built-in user model
    # is registered.
    admin.site.unregister(get_user_model())
    admin.site.register(get_user_model(), UserAdmin)
    urlpatterns.append(path('superadmin/', admin.site.urls))
urlpatterns += [i[1] for i in BROWSABLE_API_URLS]
urlpatterns += static(settings.STATIC_URL, document_root=settings.STATIC_ROOT)
