import Head from 'next/head'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import DataSourcesAddForm from './Form'

const DataSourcesAdd = () => {
  return (
    <>
      <Head>
        <title>Create Data Source</title>
      </Head>

      <PageTitle>Data Sources</PageTitle>

      <Tabs
        tabs={[
          { title: 'View All', href: '/[projectId]/data-sources' },
          {
            title: 'Create Data Source',
            href: '/[projectId]/data-sources/add',
          },
        ]}
      />

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <DataSourcesAddForm />
      </SuspenseBoundary>
    </>
  )
}

export default DataSourcesAdd
