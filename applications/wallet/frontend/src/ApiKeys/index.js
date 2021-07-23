import Head from 'next/head'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { spacing, typography, colors } from '../Styles'

import PageTitle from '../PageTitle'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Tabs from '../Tabs'
import Table, { ROLES } from '../Table'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import ApiKeysCopy from './Copy'
import ApiKeysRow from './Row'

const ApiKeys = () => {
  const {
    query: { projectId, action },
  } = useRouter()

  return (
    <>
      <Head>
        <title>API Keys</title>
      </Head>

      <PageTitle>Project API Keys</PageTitle>

      {action === 'delete-apikey-success' && (
        <div css={{ display: 'flex', paddingTop: spacing.base }}>
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            API Key deleted.
          </FlashMessage>
        </div>
      )}

      <ApiKeysCopy />

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/api-keys' },
          { title: 'Create API key', href: '/[projectId]/api-keys/add' },
        ]}
      />

      <Table
        role={ROLES.API_Keys}
        legend="Keys"
        url={`/api/v1/projects/${projectId}/api_keys/`}
        refreshKeys={[]}
        refreshButton={false}
        columns={['API Key Name', 'Permissions', '#Actions#']}
        expandColumn={2}
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
              There are currently no API keys.
            </div>

            <div css={{ display: 'flex' }}>
              <Link href={`/${projectId}/api-keys/add`} passHref>
                <Button variant={BUTTON_VARIANTS.PRIMARY}>
                  Create an API Key
                </Button>
              </Link>
            </div>
          </div>
        }
        renderRow={({ result, revalidate }) => (
          <ApiKeysRow
            key={result.id}
            projectId={projectId}
            apiKey={result}
            revalidate={revalidate}
          />
        )}
      />
    </>
  )
}

export default ApiKeys
