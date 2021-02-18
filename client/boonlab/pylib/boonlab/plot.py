import os
import cv2
from MulticoreTSNE import MulticoreTSNE as TSNE
from sklearn.cluster import KMeans
from sklearn.metrics import pairwise_distances_argmin_min

from bokeh.plotting import figure, show
from bokeh.palettes import Paired12, Spectral11
from bokeh.models import ColumnDataSource, HoverTool, PanTool, WheelZoomTool
from bokeh.models import ResetTool, SaveTool, BoxZoomTool

import holoviews as hv

from boonsdk import app_from_env
from .hashes import read_hash_as_vectors
from .proxies import download_proxy
from .display import is_running_from_ipython


def plot_tsne(search=None,
              attr='analysis.boonai-image-similarity.simhash',
              percentage=100,
              maxPoints=10000,
              nClusters=1,
              verbose=True,
              thumbs=False):
    """Plots a t-sne graph of the search provided.

    Returns an array of assets, the 2D points, and an array of cluster indices.
    """

    hv.extension('bokeh')
    app = app_from_env()

    if search is None:
        search = app.assets.search({"size": 10000})

    if len(search.assets) < 10:
        print("Less than 10 assets, returning!")
        return None, None, None

    if len(search.assets) * percentage / 100 > maxPoints:
        percentage = maxPoints * 100 / len(search.assets)

    x, assets = read_hash_as_vectors(attr=attr, search=search,
                                     percentage=percentage, verbose=verbose)

    xtsne = TSNE(n_components=2,
                 random_state=42,
                 metric='cityblock',
                 verbose=verbose).fit_transform(x)

    imgs = []

    if thumbs:
        tmpdir = 'tmp/'
        if not os.path.isdir(tmpdir):
            os.mkdir(tmpdir)
        for a in assets:
            name = 'tmp/' + a.id + '.jpg'
            fullname = './' + name
            if not os.path.isfile(fullname):
                img = download_proxy(a, 0)
                if img is None:
                    continue
                img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
                cv2.imwrite(name, img)

            imgs.append(name)

    legend = []
    kmeans = KMeans(n_clusters=nClusters, random_state=0).fit(x)

    if is_running_from_ipython():
        closest, _ = pairwise_distances_argmin_min(kmeans.cluster_centers_, x)
        PAL = Paired12 + Spectral11
        colormap = []

        for label in kmeans.labels_:
            colormap.append(PAL[label])

        for a in assets:
            legend.append(a.get_attr('source.filename'))

        TOOLS = [BoxZoomTool, PanTool, WheelZoomTool, ResetTool, SaveTool]

        radius = 20

        if thumbs:
            source = ColumnDataSource(data=dict(
                x=xtsne[:, 0],
                y=xtsne[:, 1],
                imgs=imgs,
                desc=legend,
                colors=colormap,
                sz=[0.01 * radius] * len(colormap)
            ))

            hover = HoverTool(
                tooltips="""
                    <div>
                    <div>
                            <img
                                src=@imgs height="130" alt="@imgs" width="200"
                                style="float: left; margin: 5px 5px 5px 5px;"
                                border="2">
                    </img>
                    </div>
                    </div>
                    """
            )
        else:
            source = ColumnDataSource(data=dict(
                x=xtsne[:, 0],
                y=xtsne[:, 1],
                desc=legend,
                cluster=kmeans.labels_,
                colors=colormap,
                sz=[0.01 * radius] * len(colormap)
            ))

            hover = HoverTool(tooltips=[('Cluster', '@cluster'),
                                        ('Filename', '@desc')])

        tools = [t() for t in TOOLS] + [hover]

        p = figure(tools=tools, plot_width=800, plot_height=600)
        p.background_fill_color = "black"
        p.background_fill_alpha = 0.8
        p.grid.visible = False

        p.circle('x', 'y', fill_color='colors', radius='sz', source=source,
                 line_color=None)

        show(p)

    return assets, x, kmeans.labels_
