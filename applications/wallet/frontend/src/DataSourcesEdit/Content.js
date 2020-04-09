import useSWR from 'swr'
import { useRouter } from 'next/router'

import { FILE_TYPES } from '../DataSourcesAdd/helpers'

import DataSourcesForm from './Form'

const DataSourcesEditContent = () => {
  const {
    query: { projectId, dataSourceId },
  } = useRouter()

  const {
    data: { name, uri, credentials, fileTypes, modules },
  } = useSWR(`/api/v1/projects/${projectId}/data_sources/${dataSourceId}`)

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
    credentials,
    fileTypes: groupedFileTypes,
    modules,
    errors: { global: '' },
  }

  return <DataSourcesForm initialState={initialState} />
}

export default DataSourcesEditContent
