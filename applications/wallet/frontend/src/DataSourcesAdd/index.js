import Head from 'next/head'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'

import DataSourcesAddForm from './Form'

const DataSourcesAdd = () => {
  return (
    <>
      <Head>
        <title>Add Data Source</title>
      </Head>

      <PageTitle>Data Sources</PageTitle>

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/data-sources' },
          { title: 'Add Data Source', href: '/[projectId]/data-sources/add' },
        ]}
      />

      <DataSourcesAddForm />
    </>
  )
}

export default DataSourcesAdd
