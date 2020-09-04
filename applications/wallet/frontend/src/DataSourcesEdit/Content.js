import useSWR from 'swr'
import { useRouter } from 'next/router'

import DataSourcesEditForm from './Form'

const DataSourcesEditContent = () => {
  const {
    query: { projectId, dataSourceId },
  } = useRouter()

  const {
    data: { name, uri, fileTypes, modules, credentials },
  } = useSWR(`/api/v1/projects/${projectId}/data_sources/${dataSourceId}/`)

  const initialState = {
    name,
    uri,
    fileTypes: fileTypes.reduce(
      (accumulator, fileType) => ({ ...accumulator, [fileType]: true }),
      {},
    ),
    modules,
    credentials,
    isLoading: false,
    errors: {},
  }

  return <DataSourcesEditForm initialState={initialState} />
}

export default DataSourcesEditContent
