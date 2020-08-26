import Head from 'next/head'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import ModelsAddForm from './Form'

const Models = () => {
  return (
    <>
      <Head>
        <title>Create New Model</title>
      </Head>

      <PageTitle>Create New Model</PageTitle>

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/models' },
          { title: 'Create New Model', href: '/[projectId]/models/add' },
        ]}
      />

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <ModelsAddForm />
      </SuspenseBoundary>
    </>
  )
}

export default Models
