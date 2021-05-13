import Head from 'next/head'

import PageTitle from '../PageTitle'
import BetaBadge from '../BetaBadge'
import Tabs from '../Tabs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import ModelsAddForm from './Form'

const Models = () => {
  return (
    <>
      <Head>
        <title>Custom Models</title>
      </Head>

      <PageTitle>
        Custom Models
        <BetaBadge isLeft={false} />
      </PageTitle>

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
