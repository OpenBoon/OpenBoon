import Head from 'next/head'

import PageTitle from '../PageTitle'
import BetaBadge from '../BetaBadge'
import Tabs from '../Tabs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import DatasetsAddForm from './Form'

const Datasets = () => {
  return (
    <>
      <Head>
        <title>Datasets</title>
      </Head>

      <PageTitle>
        <BetaBadge isLeft />
        Datasets
      </PageTitle>

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/datasets' },
          { title: 'Create New Dataset', href: '/[projectId]/datasets/add' },
        ]}
      />

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <DatasetsAddForm />
      </SuspenseBoundary>
    </>
  )
}

export default Datasets
