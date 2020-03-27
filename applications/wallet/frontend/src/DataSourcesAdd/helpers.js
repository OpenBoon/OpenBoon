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

export const PROVIDERS = {
  Zorroa: {
    logo: <ZorroaLogoSvg height={32} />,
    description: (
      <span>
        These analysis modules are included in your base package. You can run as
        many as you’d like, but running more than you need will increase
        processing time.
      </span>
    ),
    categories: {
      'Images & Documents': {},
      Video: {},
    },
  },
  'Google Cloud': {
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
    categories: {
      'Google Vision (Images & Documents)': {},
      'Google Video': {},
    },
  },
}

export const formatModules = ({ modules }) => {
  modules.forEach((module) => {
    if (module.provider === 'Zorroa') {
      if (
        module.supportedMedia.length === 1 &&
        module.supportedMedia[0] === 'Video'
      ) {
        PROVIDERS.Zorroa.categories.Video[module.name] = {
          value: module.name,
          label: module.name,
          legend: module.description,
          initialValue: false,
          isDisabled: module.restricted,
        }
      } else {
        PROVIDERS.Zorroa.categories['Images & Documents'][module.name] = {
          value: module.name,
          label: module.name,
          legend: module.description,
          initialValue: false,
          isDisabled: module.restricted,
        }
      }
    }
  })

  return PROVIDERS
}

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
          .filter((f) => fileTypes[f])
          .flatMap((f) => {
            const { legend: extensions } = FILE_TYPES.find(
              ({ value }) => value === f,
            )
            return extensions.toLowerCase().split(',')
          }),
        modules: Object.keys(modules).filter((m) => modules[m]),
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
