import Head from 'next/head'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import WebhooksAddForm from './Form'

const WebhooksAdd = () => {
  return (
    <>
      <Head>
        <title>Create Webhook</title>
      </Head>

      <PageTitle>Webhooks</PageTitle>

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/webhooks' },
          { title: 'Create Webhook', href: '/[projectId]/webhooks/add' },
        ]}
      />

      <SuspenseBoundary role={ROLES.Webhooks}>
        <WebhooksAddForm />
      </SuspenseBoundary>
    </>
  )
}

export default WebhooksAdd
