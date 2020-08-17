import Head from 'next/head'
import { useRouter } from 'next/router'

import { spacing } from '../Styles'

import Breadcrumbs from '../Breadcrumbs'
import FlashMessage, { VARIANTS } from '../FlashMessage'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'
import ModelLabels from '../ModelLabels'
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

      {action === 'edit-label-success' && (
        <div
          css={{
            display: 'flex',
            paddingBottom: spacing.normal,
          }}
        >
          <FlashMessage variant={VARIANTS.SUCCESS}>Label updated.</FlashMessage>
        </div>
      )}

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <ModelDetails key={pathname} />

        {!edit && <ModelLabels />}

        {edit && (
          <LabelEdit projectId={projectId} modelId={modelId} label={edit} />
        )}
      </SuspenseBoundary>
    </>
  )
}

export default Model
