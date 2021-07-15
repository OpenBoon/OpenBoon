#!/usr/bin/env python3
"""

This tool is used by the processing system to download the files
in a dataset in parallel.

"""
import argparse
import logging
from multiprocessing import Pool

import boonsdk
from boonsdk.training import TrainingSetDownloader


def main():

    logging.basicConfig(level=logging.INFO)

    parser = argparse.ArgumentParser(description='Download some files')
    parser.add_argument('dataset', help="The DataSet Id")
    parser.add_argument('style', help="The style to write the dataset into.")
    parser.add_argument('dst_dir', help="The dir to download to.")
    parser.add_argument('-t', '--threads', type=int, default=8,
                        help="The number of threads to download with. Defaults to 8")
    parser.add_argument('-r', '--validation-split', type=float, default=0.2,
                        help="The number of images for training set vs test set, defaults to 4")

    args = parser.parse_args()
    app = boonsdk.app_from_env()

    with Pool(processes=args.threads) as pool:
        dl = TrainingSetDownloader(app, args.dataset, args.style, args.dst_dir,
                                   validation_split=args.validation_split)
        dl.build(pool=pool)
        pool.close()
        pool.join()


if __name__ == '__main__':
    main()
