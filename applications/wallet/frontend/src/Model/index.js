import { useState } from 'react'
import Head from 'next/head'
import { useRouter } from 'next/router'

import { spacing } from '../Styles'

import Breadcrumbs from '../Breadcrumbs'
import FlashMessageErrors from '../FlashMessage/Errors'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'
import FetchAhead from '../Fetch/Ahead'

import ModelContent from './Content'

const Model = () => {
  const [errors, setErrors] = useState({})

  const {
    query: { projectId, action },
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

      {action === 'add-model-success' && (
        <div
          css={{
            display: 'flex',
            paddingTop: spacing.base,
            paddingBottom: spacing.normal,
          }}
        >
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            Model created.
          </FlashMessage>
        </div>
      )}

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
        <FetchAhead
          url={`/api/v1/projects/${projectId}/datasets/dataset_types/`}
        />

        <FetchAhead url={`/api/v1/projects/${projectId}/datasets/`} />

        <ModelContent setErrors={setErrors} />
      </SuspenseBoundary>
    </>
  )
}

export default Model
