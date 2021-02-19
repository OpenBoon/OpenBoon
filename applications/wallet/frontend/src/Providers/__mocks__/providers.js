const providers = {
  results: [
    {
      name: 'zorroa',
      logo:
        'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDMuMyAzMCI+CiAgPHBhdGggZD0iTTExLjMgMGwxMS45IDQuNy0uOCAxLjd6bS0uMy45TDIzLjUgOGwtNy40IDEyLjctMi40LTEuNiA3LjgtOS42eiIgZmlsbD0iI2ZmZmZmZiIvPgogIDxwYXRoIGQ9Ik05LjQuOGwxMC44IDkuMS05LjcgMTEtMS44LTEuNyA5LjYtOC4zeiIgZmlsbD0iI2I4ZDY1MiIvPgogIDxwYXRoIGQ9Ik0xMy43IDkuNEwxLjEgMTEuOGwuNCAxLjd6bS0uMSAxTDAgMTQuOWw0LjcgMTMuOSAyLjgtMS4xLTUuOC0xMXoiIGZpbGw9IiNmZmZmZmYiLz4KICA8cGF0aCBkPSJNMTUuMSAxMC41TDIuOSAxNy40IDEwLjMgMzBsMi4xLTEuMy03LjktMTB6IiBmaWxsPSIjYjhkNjUyIi8+CiAgPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTk5LjMgMTAuN2gtMmwtMy45IDkuOWgybDEtMi40aDMuOGwxIDIuNGgybC0zLjktOS45em0uMyA1LjhIOTdsMS4zLTMuOSAxLjMgMy45em0tMjIuMy0xLjJjLjQtLjUuNi0xIC42LTEuNyAwLTEtLjUtMS43LTEuMy0yLjMtLjYtLjMtMS41LS41LTIuNi0uNWgtNC4zdjkuOWgxLjl2LTQuMWgyLjZsMiA0LjFoMkw3NiAxNi4yYy41LS4yLjktLjUgMS4zLS45ek03NiAxMy42YzAgLjUtLjEuOC0uNSAxLS4yLjEtLjUuMy0uNy40LS4zLjEtLjYuMS0xIC4xaC0yLjN2LTIuOUg3NGMuNiAwIDEgLjEgMS40LjIuNC40LjYuNy42IDEuMnptLTExIDEuN2MuNC0uNS42LTEgLjYtMS43IDAtMS0uNS0xLjctMS4zLTIuMy0uNi0uNC0xLjUtLjYtMi42LS42aC00LjN2OS45aDEuOXYtNC4xSDYybDIgNC4xaDJsLTIuMi00LjRjLjUtLjIuOS0uNSAxLjItLjl6bS0xLjMtMS43YzAgLjUtLjEuOC0uNSAxLS4yLjEtLjUuMy0uNy40LS4zLjEtLjYuMS0xIC4xaC0yLjN2LTIuOWgyLjVjLjYgMCAxIC4xIDEuNC4yLjUuNC42LjcuNiAxLjJ6bS0yMy0xLjd2LTEuMmgtNy44djEuNWg1LjRsLTUuNiA3djEuNGg4LjF2LTEuNUgzNXptMTAuMi0xYy00LTEuNC03LjkgMi41LTYuNSA2LjUuNSAxLjUgMS43IDIuNiAzLjEgMy4xIDQgMS40IDcuOS0yLjUgNi41LTYuNS0uNS0xLjQtMS43LTIuNi0zLjEtMy4xem0xLjYgNGMuNiAyLjQtMS42IDQuNy00LjEgNC4xLTEuMi0uMy0yLjEtMS4yLTIuNC0yLjQtLjYtMi40IDEuNi00LjcgNC4xLTQuMSAxLjEuMyAyIDEuMiAyLjQgMi40em0zNS40LTRjLTQtMS40LTcuOSAyLjUtNi41IDYuNS41IDEuNSAxLjcgMi42IDMuMSAzLjEgNCAxLjQgNy45LTIuNSA2LjUtNi41LS41LTEuNC0xLjctMi42LTMuMS0zLjF6bTEuNSA0Yy42IDIuNC0xLjYgNC43LTQuMSA0LjEtMS4yLS4zLTIuMS0xLjItMi40LTIuNC0uNi0yLjQgMS42LTQuNyA0LjEtNC4xIDEuMS4zIDIuMSAxLjIgMi40IDIuNHoiLz4KPC9zdmc+Cg==',
      description:
        'These analysis modules are included in your base package. You can run as many as you’d like, but running more than you need will increase processing time.',
      categories: [
        {
          name: 'Visual Intelligence',
          modules: [
            {
              id: 'be1aae20-5fdf-1d01-9a0e-4a35ed1b62b8',
              name: 'gcp-label-detection',
              description:
                'Utilize Google Cloud Vision label detection to detect and extract information about entities in an image, across a broad group of categories.',
              provider: 'Zorroa',
              category: 'Visual Intelligence',
              supportedMedia: ['Images', 'Videos', 'Documents'],
              timeCreated: 1584378384472,
              timeModified: 1585594803896,
              actorCreated:
                '00000000-1234-1234-1234-000000000000/background-thread',
              actorModified:
                '00000000-1234-1234-1234-000000000000/background-thread',
            },
            {
              id: '5b7127e5-1c94-1c14-9d23-864543129f9e',
              name: 'gcp-object-detection',
              description:
                'Utilize Google Cloud Vision label detection to detect and extract information about entities in an image, across a broad group of categories.',
              provider: 'Zorroa',
              category: 'Visual Intelligence',
              supportedMedia: ['Images', 'Videos', 'Documents'],
              timeCreated: 1584717585694,
              timeModified: 1585594803975,
              actorCreated:
                '00000000-1234-1234-1234-000000000000/background-thread',
              actorModified:
                '00000000-1234-1234-1234-000000000000/background-thread',
            },
            {
              id: '006592d6-5555-1264-99d6-12022d8ebebd',
              name: 'boonai-label-detection',
              description:
                'Generate keyword labels for image, video, and documents.',
              provider: 'Zorroa',
              category: 'Visual Intelligence',
              supportedMedia: ['Images', 'Videos', 'Documents'],
              timeCreated: 1583176293058,
              timeModified: 1585594803772,
              actorCreated:
                '00000000-1234-1234-1234-000000000000/background-thread',
              actorModified:
                '00000000-1234-1234-1234-000000000000/background-thread',
            },
            {
              id: '006592d5-5555-1264-99d6-12022d8ebebd',
              name: 'boonai-object-detection',
              description:
                'Detect everyday objects in images, video, and documents.',
              provider: 'Zorroa',
              category: 'Visual Intelligence',
              supportedMedia: ['Images', 'Videos', 'Documents'],
              timeCreated: 1583176293039,
              timeModified: 1585594803696,
              actorCreated:
                '00000000-1234-1234-1234-000000000000/background-thread',
              actorModified:
                '00000000-1234-1234-1234-000000000000/background-thread',
            },
          ],
        },
        {
          name: 'Zorroa Timeline Extraction',
          modules: [
            {
              id: '2357b0f6-4672-1cf3-9d61-0af5076e65af',
              name: 'boonai-document-page-clips',
              description:
                'Extract all pages in MS Office/PDF documents into separate assets.',
              provider: 'Zorroa',
              category: 'Zorroa Timeline Extraction',
              supportedMedia: ['Documents'],
              timeCreated: 1585355679147,
              timeModified: 1585594803394,
              actorCreated:
                '00000000-1234-1234-1234-000000000000/background-thread',
              actorModified:
                '00000000-1234-1234-1234-000000000000/background-thread',
            },
            {
              id: '2357b0f7-4672-1cf3-9d61-0af5076e65af',
              name: 'boonai-image-page-clips',
              description:
                'Extract all layers in multi page image formats such as tiff and psd as as separate assets',
              provider: 'Zorroa',
              category: 'Zorroa Timeline Extraction',
              supportedMedia: ['Images'],
              timeCreated: 1585355679820,
              timeModified: 1585594803589,
              actorCreated:
                '00000000-1234-1234-1234-000000000000/background-thread',
              actorModified:
                '00000000-1234-1234-1234-000000000000/background-thread',
            },
            {
              id: '2357b0f8-4672-1cf3-9d61-0af5076e65af',
              name: 'boonai-video-shot-clips',
              description:
                'Break video files into individual assets based on a shot detection algorithm.',
              provider: 'Zorroa',
              category: 'Zorroa Timeline Extraction',
              supportedMedia: ['Videos'],
              timeCreated: 1585355679835,
              timeModified: 1585594803678,
              actorCreated:
                '00000000-1234-1234-1234-000000000000/background-thread',
              actorModified:
                '00000000-1234-1234-1234-000000000000/background-thread',
            },
          ],
        },
        {
          name: 'Zorroa Visual Intelligence',
          modules: [
            {
              id: '2357b0f9-4672-1cf3-9d61-0af5076e65af',
              name: 'boonai-text-detection',
              description: 'Utilize OCR technology to detect text on an image.',
              provider: 'Zorroa',
              category: 'Zorroa Visual Intelligence',
              supportedMedia: ['Images'],
              timeCreated: 1585355680037,
              timeModified: 1585594803787,
              actorCreated:
                '00000000-1234-1234-1234-000000000000/background-thread',
              actorModified:
                '00000000-1234-1234-1234-000000000000/background-thread',
            },
          ],
        },
      ],
    },
  ],
}

export default providers
