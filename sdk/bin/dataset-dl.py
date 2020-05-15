#!/usr/bin/env python3
"""

This tool is used by the processing system to download the files
in a dataset in parallel.

"""
import argparse
import logging
from multiprocessing import Pool

import zmlp


def main():

    logging.basicConfig(level=logging.INFO)

    parser = argparse.ArgumentParser(description='Download some files')
    parser.add_argument('format', help="The format to write the dataset into.")
    parser.add_argument('dataset', help="The dataset Id")
    parser.add_argument('dst_dir', help="The dir to download to.")
    parser.add_argument('-t', '--threads', type=int, default=8,
                        help="The number of threads to download with. Defaults to 8")
    parser.add_argument('-r', '--train-test-ratio', type=int, default=3,
                        help="The number of images for training set vs test set, defaults to 3")

    args = parser.parse_args()
    app = zmlp.app_from_env()

    with Pool(processes=args.threads) as pool:
        dl = zmlp.app.dataset_app.DataSetDownloader(
            app, args.dataset, args.dst_dir, train_test_ratio=args.train_test_ratio)
        dl.build(args.format, pool=pool)
        pool.close()
        pool.join()


if __name__ == '__main__':
    main()
