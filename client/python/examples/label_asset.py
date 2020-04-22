#!/usr/bin/env python3
import argparse

from zmlp import app_from_env

app = app_from_env()


def main():
    parser = argparse.ArgumentParser(description='Label Asset')

    parser.add_argument('path', help="The path to the asset")
    parser.add_argument('dataset', help="The dataset name")
    parser.add_argument('label', help="The label")

    args = parser.parse_args()

    assets = app.assets.search({"query": {"term": {"source.path": args.path}}}).assets
    dataset = app.datasets.find_one_dataset(name=args.dataset)
    app.assets.update_labels(assets, dataset.make_label(args.label))

    print("Labels")
    print("---------------------------------------------------")
    print("%-25s count" % "Label")
    print("---------------------------------------------------")
    for label, count in app.datasets.get_label_counts(dataset).items():
        print("%-25s %d" % (label, count))


if __name__ == '__main__':
    main()
