import Head from 'next/head'
import { useRouter } from 'next/router'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'
import Table, { ROLES } from '../Table'

import ModelsCopy from './Copy'
import ModelsEmpty from './Empty'
import ModelsRow from './Row'

const Models = () => {
  const {
    query: { projectId },
  } = useRouter()

  return (
    <>
      <Head>
        <title>Custom Models</title>
      </Head>

      <PageTitle>Custom Models</PageTitle>

      <ModelsCopy projectId={projectId} />

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/models' },
          { title: 'Create New Model', href: '/[projectId]/models/add' },
        ]}
      />

      <Table
        role={ROLES.ML_Tools}
        legend="Models"
        url={`/api/v1/projects/${projectId}/models/`}
        refreshKeys={[]}
        refreshButton={false}
        columns={['Name', 'Count']}
        expandColumn={1}
        renderEmpty={<ModelsEmpty />}
        renderRow={({ result }) => (
          <ModelsRow key={result.id} projectId={projectId} model={result} />
        )}
      />
    </>
  )
}

export default Models
