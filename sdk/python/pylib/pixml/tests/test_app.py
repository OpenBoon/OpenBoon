
from pixml import PixmlAsset

app = pixml.App(apikey="abc123")
asset1 = AssetBuilder("/foo/bar/cat.jpg")
asset1.set_attr("count", 1)

app.bulk_process_assets([asset1], analysis="test")




app.bulk_index_images(asset1)

proxy1 = asset.get_file_storage(
    "proxy", "proxy_200x200.jpg", attrs={"width": 200, "height": "200"})
proxy1.get_local_path
proxy1.url
proxy1.store()

proxy1.get_local_path()

asset1.add_file(prox1)
