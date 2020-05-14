import useSWR from 'swr'
import { useRouter } from 'next/router'

import { FILE_TYPES } from '../DataSourcesAdd/helpers'

import DataSourcesEditForm from './Form'

const DataSourcesEditContent = () => {
  const {
    query: { projectId, dataSourceId },
  } = useRouter()

  const {
    data: { name, uri, fileTypes, modules, credentials },
  } = useSWR(`/api/v1/projects/${projectId}/data_sources/${dataSourceId}/`)

  const groupedFileTypes = FILE_TYPES.reduce(
    (accumulator, { value, identifier }) => {
      const matchFileType = fileTypes.includes(identifier)

      if (matchFileType) {
        accumulator[value] = true
      }

      return accumulator
    },
    {},
  )

  const initialState = {
    name,
    uri,
    fileTypes: groupedFileTypes,
    modules,
    credentials,
    errors: { global: '' },
  }

  return <DataSourcesEditForm initialState={initialState} />
}

export default DataSourcesEditContent
