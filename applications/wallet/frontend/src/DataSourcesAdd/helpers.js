import Router from 'next/router'

import { colors } from '../Styles'

import {
  fetcher,
  revalidate,
  getQueryString,
  parseResponse,
} from '../Fetch/helpers'

export const FILE_TYPES = [
  {
    value: 'Images',
    label: 'Image Files',
    extensions: 'GIF, PNG, JPG, JPEG, TIF, TIFF, PSD',
    icon: '/icons/images.png',
    color: colors.signal.canary.base,
  },
  {
    value: 'Documents',
    label: 'Documents (PDF & MS Office)',
    legend: 'Pages will be processed and counted as individual assets',
    extensions: 'PDF, DOC, DOCX, PPT, PPTX, XLS, XLSX',
    icon: '/icons/documents.png',
    color: colors.graph.seafoam,
  },
  {
    value: 'Videos',
    label: 'Video Files',
    extensions: 'MP4, M4V, MOV, MPG, MPEG, OGG',
    icon: '/icons/videos.png',
    color: colors.graph.lilac,
  },
]

export const onSubmit = async ({
  dispatch,
  projectId,
  state: { name, uri, credentials, source, fileTypes, modules },
}) => {
  dispatch({ isLoading: true, errors: {} })

  const parsedCredentials = Object.keys(credentials[source]).reduce(
    (acc, credential) => {
      const { value } = credentials[source][credential]
      if (value) {
        acc[credential] = value
      }
      return acc
    },
    {},
  )

  try {
    const { jobId } = await fetcher(
      `/api/v1/projects/${projectId}/data_sources/`,
      {
        method: 'POST',
        body: JSON.stringify({
          name,
          uri,
          credentials: Object.keys(parsedCredentials).length
            ? { type: source, ...parsedCredentials }
            : {},
          fileTypes: Object.keys(fileTypes).filter((f) => fileTypes[f]),
          modules: Object.keys(modules).filter((m) => modules[m]),
        }),
      },
    )

    await revalidate({
      key: `/api/v1/projects/${projectId}/data_sources/`,
      paginated: true,
    })

    const queryString = getQueryString({
      action: 'add-datasource-success',
      jobId,
    })

    Router.push(
      `/[projectId]/data-sources${queryString}`,
      `/${projectId}/data-sources`,
    )
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}
