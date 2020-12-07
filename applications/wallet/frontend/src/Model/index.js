import Head from 'next/head'
import { useRouter } from 'next/router'

import { spacing } from '../Styles'

import Breadcrumbs from '../Breadcrumbs'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import LabelEdit from '../LabelEdit'

import ModelDetails from './Details'

const Model = () => {
  const {
    pathname,
    query: { projectId, modelId, edit = '', action },
  } = useRouter()

  return (
    <>
      <Head>
        <title>Model Details</title>
      </Head>

      <Breadcrumbs
        crumbs={[
          { title: 'Custom Models', href: '/[projectId]/models' },
          { title: 'Model Details', href: false },
        ]}
      />

      {!!action && (
        <div css={{ display: 'flex', paddingBottom: spacing.normal }}>
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            {action === 'edit-label-success' && 'Label updated.'}
            {action === 'delete-label-success' && 'Label deleted.'}
          </FlashMessage>
        </div>
      )}

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <ModelDetails key={pathname} />

        {pathname === '/[projectId]/models/[modelId]' && edit && (
          <LabelEdit projectId={projectId} modelId={modelId} label={edit} />
        )}
      </SuspenseBoundary>
    </>
  )
}

export default Model
