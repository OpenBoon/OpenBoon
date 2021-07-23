import Head from 'next/head'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { spacing, typography, colors } from '../Styles'

import PageTitle from '../PageTitle'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Tabs from '../Tabs'
import Table, { ROLES } from '../Table'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

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

      {!!action && (
        <div css={{ display: 'flex', paddingTop: spacing.base }}>
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            Webhook
            {action === 'add-webhook-success' && ' created.'}
            {action === 'edit-webhook-success' && ' edited.'}
            {action === 'delete-webhook-success' && ' deleted.'}
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
        renderEmpty={
          <div
            css={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
            }}
          >
            <div
              css={{
                fontSize: typography.size.colossal,
                lineHeight: typography.height.colossal,
                fontWeight: typography.weight.bold,
                color: colors.structure.white,
                paddingBottom: spacing.normal,
              }}
            >
              There are currently no webhooks.
            </div>

            <div css={{ display: 'flex' }}>
              <Link href={`/${projectId}/webhooks/add`} passHref>
                <Button variant={BUTTON_VARIANTS.PRIMARY}>
                  Create a Webhook
                </Button>
              </Link>
            </div>
          </div>
        }
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
