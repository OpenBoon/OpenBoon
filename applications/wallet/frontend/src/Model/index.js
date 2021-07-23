import { useState } from 'react'
import Head from 'next/head'
import { useRouter } from 'next/router'

import { spacing } from '../Styles'

import Breadcrumbs from '../Breadcrumbs'
import FlashMessageErrors from '../FlashMessage/Errors'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'
import Tabs from '../Tabs'

import ModelDataset from '../ModelDataset'
// import ModelUpload from '../ModelUpload'

import ModelContent from './Content'

const Model = () => {
  const [errors, setErrors] = useState({})

  const {
    query: { action },
  } = useRouter()

  return (
    <>
      <Head>
        <title>Model Details</title>
      </Head>

      <Breadcrumbs
        crumbs={[
          { title: 'Custom Models', href: '/[projectId]/models', isBeta: true },
          { title: 'Model Details', href: false },
        ]}
      />

      <FlashMessageErrors
        errors={errors}
        styles={{ paddingTop: spacing.base, paddingBottom: spacing.normal }}
      />

      {['link-dataset-success', 'unlink-dataset-success'].includes(action) && (
        <div
          css={{
            display: 'flex',
            paddingTop: spacing.base,
            paddingBottom: spacing.normal,
          }}
        >
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            Dataset {action === 'link-dataset-success' ? 'linked' : 'unlinked'}.
          </FlashMessage>
        </div>
      )}

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <ModelContent />

        {/* <ModelUpload /> */}

        <Tabs
          tabs={[{ title: 'Dataset', href: '/[projectId]/models/[modelId]' }]}
        />

        <ModelDataset setErrors={setErrors} />
      </SuspenseBoundary>
    </>
  )
}

export default Model
