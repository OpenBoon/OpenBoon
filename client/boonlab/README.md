# ZVI Library

ZVI is a collection of functions that can be used for various purposes ranging from returning a pandas DataFrame from a search result to T-SNE plotting. 

Some functions work differently if being run in a Jupyter notebook. For example, plot.py will display the T-SNE plot if within a notebook otherwise returns an array of assets, the 2D points, and an array of cluster indices.

## List of Functions
**Updated 06/16/2020**<br>
List will continue to be updated as functions are added.


`show_thumbnails`
    - displays the thumbnails for the first page of assets for a given search
    
`show_asset`
    - downloads and displays the largest proxy for an asset

`read_hash_as_vectors`
    - reads a similarity hash from a Zorroa server into a numpy array

`search_to_df`
    - convert search results to DataFrame

`plot_tsne`
    - plots a t-sne graph of the search provided

`download_proxy`
    - download image proxy