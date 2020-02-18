import Head from 'next/head'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'
import Loading from '../Loading'

import DataSourcesEditForm from './Form'

const DataSourcesEdit = () => {
  const {
    query: { projectId, dataSourceId },
  } = useRouter()

  const { data: dataSource = {} } = useSWR(
    `/api/v1/projects/${projectId}/datasources/${dataSourceId}`,
  )

  return (
    <>
      <Head>
        <title>Edit Data Source</title>
      </Head>

      <PageTitle>Data Sources</PageTitle>

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/data-sources' },
          { title: 'Add Data Source', href: '/[projectId]/data-sources/add' },
          {
            title: 'Edit',
            href: '/[projectId]/data-sources/[dataSourceId]/edit',
          },
        ]}
      />

      {!dataSource.id ? (
        <Loading />
      ) : (
        <DataSourcesEditForm projectId={projectId} dataSource={dataSource} />
      )}
    </>
  )
}

export default DataSourcesEdit
