from djangorestframework_camel_case.parser import CamelCaseJSONParser
from djangorestframework_camel_case.render import CamelCaseBrowsableAPIRenderer, \
    CamelCaseJSONRenderer


class ConvertCamelToSnakeViewSetMixin(object):
    """Mixin for DRF viewsets that overrides the parsers and renders so that all camelCase
    parameters sent to the view are converted to snake_case. Helpful for views that interact
    with Wallet models that use snake case.

    NOTE: This mixin needs to come first when defining a class.

    """
    renderer_classes = [CamelCaseJSONRenderer, CamelCaseBrowsableAPIRenderer]
    parser_classes = [CamelCaseJSONParser]
