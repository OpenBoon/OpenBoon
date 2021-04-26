import Head from 'next/head'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import WebhooksEditForm from './Form'

const WebhooksEdit = () => {
  return (
    <>
      <Head>
        <title>Edit Webhook</title>
      </Head>

      <PageTitle>Webhooks</PageTitle>

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/webhooks' },
          { title: 'Create Webhook', href: '/[projectId]/webhooks/add' },
          {
            title: 'Edit Webhook',
            href: '/[projectId]/webhooks/[webhookId]/edit',
          },
        ]}
      />

      <SuspenseBoundary role={ROLES.Webhooks}>
        <WebhooksEditForm />
      </SuspenseBoundary>
    </>
  )
}

export default WebhooksEdit
