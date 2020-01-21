#!/usr/bin/env python

import sys
import argparse
import json
import base64


def main():

    parser = argparse.ArgumentParser(description='Dump an apikey string from a json key file.')
    parser.add_argument('key_file', metavar='key_file', type=str, nargs=1,
                        help='Path to the api key json file to encode.')

    args = parser.parse_args()
    if args.key_file:
        key_file = args.key_file[0]
    else:
        print('Please specify a key file to encode.')
        sys.exit(1)

    with open(key_file, 'rb') as _file:
        key_contents = json.load(_file)

    encoded = base64.b64encode(json.dumps(key_contents).encode('utf-8'))
    print('Here\'s the B64 encoded api key string:')
    print('---------------------------------------\n')
    print(encoded.decode('utf-8'))


if __name__ == '__main__':
    main()

