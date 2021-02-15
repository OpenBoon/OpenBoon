#!/usr/bin/env python3
import argparse

from boonsdk import app_from_env

app = app_from_env()


def main():
    parser = argparse.ArgumentParser(description='Label Asset')

    parser.add_argument('id', help="Asset id")
    parser.add_argument('model', help="The model name")
    parser.add_argument('label', help="The label")

    args = parser.parse_args()

    model = app.models.find_one_model(name=args.model)
    app.assets.update_labels(args.id, model.make_label(args.label))

    print("Labels")
    print("---------------------------------------------------")
    print("%-25s count" % "Label")
    print("---------------------------------------------------")
    for label, count in app.models.get_label_counts(model).items():
        print("%-25s %d" % (label, count))


if __name__ == '__main__':
    main()
