import Head from 'next/head'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import DataSourcesEditContent from './Content'

const DataSourcesEdit = () => {
  return (
    <>
      <Head>
        <title>Edit Data Source</title>
      </Head>

      <PageTitle>Data Sources</PageTitle>

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/data-sources' },
          {
            title: 'Create Data Source',
            href: '/[projectId]/data-sources/add',
          },
          {
            title: 'Edit Data Source',
            href: '/[projectId]/data-sources/[dataSourceId]/edit',
          },
        ]}
      />

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <DataSourcesEditContent />
      </SuspenseBoundary>
    </>
  )
}

export default DataSourcesEdit
