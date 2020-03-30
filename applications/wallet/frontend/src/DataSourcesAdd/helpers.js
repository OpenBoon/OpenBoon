import Router from 'next/router'

import { fetcher } from '../Fetch/helpers'

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
