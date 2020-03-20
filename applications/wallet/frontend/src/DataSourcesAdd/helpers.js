import Router from 'next/router'

import { fetcher } from '../Fetch/helpers'

import { colors } from '../Styles'

import ZorroaLogoSvg from '../Icons/logo.svg'
import GoogleCloudSvg from '../Icons/googleCloud.svg'

export const FILE_TYPES = [
  {
    value: 'images',
    label: 'Image Files',
    legend: 'GIF, PNG, JPG, JPEG, TIF, TIFF, PSD',
    icon: '/icons/images.png',
  },
  {
    value: 'documents',
    label: 'Documents (PDF & MS Office)',
    legend: 'PDF, DOC, DOCX, PPT, PPTX, XLS, XLSX',
    icon: '/icons/documents.png',
  },
  {
    value: 'video',
    label: 'Video Files',
    legend: 'MP4, M4V, MOV, MPG, MPEG, OGG',
    icon: '/icons/videos.png',
  },
]

export const MODULES = [
  {
    provider: 'Zorroa',
    logo: <ZorroaLogoSvg height={32} />,
    description: (
      <span>
        These analysis modules are included in your base package. You can run as
        many as you’d like, but running more than you need will increase
        processing time.
      </span>
    ),
    categories: [
      {
        name: 'Images & Documents',
        options: [
          {
            value: 'zmlp-classification',
            label: 'Label Detection',
            legend: 'Adds a list of predicted label that apply to the media.',
            initialValue: false,
            isDisabled: false,
          },
          {
            value: 'zmlp-objects',
            label: 'Object Detection',
            legend: 'Detects up to 80 everyday objects.',
            initialValue: false,
            isDisabled: false,
          },
          {
            value: 'zmlp-face-recognition',
            label: 'Facial Recognition',
            legend: 'Recognizes faces within an image.',
            initialValue: false,
            isDisabled: false,
          },
          {
            value: 'zmlp-ocr',
            label: 'OCR (Optical Character Recognition)',
            legend: 'Transcribes text found in images.',
            initialValue: false,
            isDisabled: false,
          },
          {
            value: 'zml-deep-document',
            label: 'Page Analysis',
            legend:
              'Breaks multipage document into individual pages and imports them as individual entities.',
            initialValue: false,
            isDisabled: false,
          },
        ],
      },
      {
        name: 'Video',
        options: [
          {
            value: 'shot-detection',
            label: 'Shot Detection',
            legend:
              'Intelligently breaks a video into separate shots and imports each as its own entity.',
            initialValue: false,
            isDisabled: false,
          },
        ],
      },
    ],
  },
  {
    provider: 'Google Cloud',
    logo: <GoogleCloudSvg height={32} />,
    description: (
      <span>
        <span css={{ color: colors.key.one }}>
          Contact your Account Manager to activate.
        </span>
        <br />
        These analysis modules call directly into the Google Cloud Vision API
        and can be activated individually.
      </span>
    ),
    categories: [
      {
        name: 'Google Vision (Images & Documents)',
        options: [
          {
            value: 'gcp-vision-crop-hints',
            label: 'Crop Hints (Vision)',
            legend:
              'Determine suggested vertices for a crop region on an image.',
            initialValue: false,
            isDisabled: true,
          },
          {
            value: 'gcp-document-text-detection',
            label: 'OCR Documents (Vision)',
            legend:
              'Perform Optical Character Recognition (OCR) on text within the image. Text detection is optimized for areas of sparse text within a larger image.',
            initialValue: false,
            isDisabled: true,
          },
          {
            value: 'gcp-vision-text-detection',
            label: 'OCR Images (Vision)',
            legend:
              'Perform Optical Character Recognition (OCR) on text within the image. Text detection is optimized for areas of sparse text within a larger image',
            initialValue: false,
            isDisabled: true,
          },
          {
            value: 'gcp-vision-label-detection',
            label: 'Label Detection (Vision)',
            legend: 'Add labels based on image content.',
            initialValue: false,
            isDisabled: true,
          },
        ],
      },
      {
        name: 'Google Video',
        options: [
          {
            value: 'gcp-video-label-detection',
            label: 'Label Detection (Video)',
            legend:
              'Identifies objects, locations, activities, animal species, products, and more.',
            initialValue: false,
            isDisabled: true,
          },
          {
            value: 'gcp-shot-detection',
            label: 'Shot Change (Video)',
            legend: 'Shot change analysis detects shot changes in a video',
            initialValue: false,
            isDisabled: true,
          },
          {
            value: 'gcp-explicit-content-detection',
            label: 'Explicit Content Detection (Video)',
            legend:
              'Explicit Content Detection detects adult content in videos. Adult content is content generally inappropriate for those under under 18 years of age and includes, but is not limited to, nudity, sexual activities, and pornography. Such content detected in cartoons or anime is also identified.',
            initialValue: false,
            isDisabled: true,
          },
        ],
      },
    ],
  },
]

export const onSubmit = async ({
  dispatch,
  projectId,
  state: { name, uri, credential, fileTypes, modules },
}) => {
  try {
    await fetcher(`/api/v1/projects/${projectId}/data_sources/`, {
      method: 'POST',
      body: JSON.stringify({
        name,
        uri,
        credential,
        file_types: Object.keys(fileTypes)
          .filter(f => fileTypes[f])
          .flatMap(f => {
            const { legend: extensions } = FILE_TYPES.find(
              ({ value }) => value === f,
            )
            return extensions.toLowerCase().split(',')
          }),
        modules: Object.keys(modules).filter(m => modules[m]),
      }),
    })

    Router.push(
      '/[projectId]/data-sources?action=add-datasource-success',
      `/${projectId}/data-sources?action=add-datasource-success`,
    )
  } catch (response) {
    const errors = await response.json()

    const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
      acc[errorKey] = errors[errorKey].join(' ')
      return acc
    }, {})

    dispatch({ errors: parsedErrors })

    window.scrollTo(0, 0)
  }
}
