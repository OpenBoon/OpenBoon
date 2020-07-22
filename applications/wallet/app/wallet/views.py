from rest_framework.routers import APIRootView


class WalletAPIRootView(APIRootView):
    "Extends the default DRF API root view to allow adding extra views."
    def get(self, request, *args, **kwargs):
        from wallet.urls import BROWSABLE_API_URLS
        for view in BROWSABLE_API_URLS:
            self.api_root_dict[view[0]] = view[1].name
        return super(WalletAPIRootView, self).get(request, *args, **kwargs)
