import Head from 'next/head'
import { useRouter } from 'next/router'

import { spacing } from '../Styles'

import PageTitle from '../PageTitle'
import BetaBadge from '../BetaBadge'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Tabs from '../Tabs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import ModelsContent from './Content'

const Models = () => {
  const {
    query: { projectId, action },
  } = useRouter()

  return (
    <>
      <Head>
        <title>Custom Models</title>
      </Head>

      <PageTitle>
        <BetaBadge isLeft />
        Custom Models
      </PageTitle>

      {action === 'add-model-success' && (
        <div css={{ display: 'flex', paddingTop: spacing.base }}>
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            Model created.
          </FlashMessage>
        </div>
      )}

      {action === 'delete-model-success' && (
        <div css={{ display: 'flex', paddingTop: spacing.base }}>
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            Model deleted.
          </FlashMessage>
        </div>
      )}

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/models' },
          { title: 'Create New Model', href: '/[projectId]/models/add' },
        ]}
      />

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <ModelsContent projectId={projectId} />
      </SuspenseBoundary>
    </>
  )
}

export default Models
