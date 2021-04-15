import Head from 'next/head'
import { useRouter } from 'next/router'

import { spacing } from '../Styles'

import PageTitle from '../PageTitle'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Tabs from '../Tabs'
import Table, { ROLES } from '../Table'

import WebhooksRow from './Row'

const Webhooks = () => {
  const {
    query: { projectId, action },
  } = useRouter()

  return (
    <>
      <Head>
        <title>Webhooks</title>
      </Head>

      <PageTitle>Webhooks</PageTitle>

      {action === 'delete-webhook-success' && (
        <div css={{ display: 'flex', paddingTop: spacing.base }}>
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            Webhook deleted.
          </FlashMessage>
        </div>
      )}

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/webhooks' },
          { title: 'Create Webhook', href: '/[projectId]/webhooks/add' },
        ]}
      />

      <Table
        role={ROLES.Webhooks}
        legend="Webhooks"
        url={`/api/v1/projects/${projectId}/webhooks/`}
        refreshKeys={[]}
        refreshButton={false}
        columns={['URL', 'Triggers', 'Active', '#Actions#']}
        expandColumn={1}
        renderEmpty="No webhook"
        renderRow={({ result, revalidate }) => (
          <WebhooksRow
            key={result.id}
            projectId={projectId}
            webhook={result}
            revalidate={revalidate}
          />
        )}
      />
    </>
  )
}

export default Webhooks
