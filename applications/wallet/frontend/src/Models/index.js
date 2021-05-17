/* eslint-disable jsx-a11y/click-events-have-key-events */
/* eslint-disable jsx-a11y/no-static-element-interactions */
import Head from 'next/head'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { spacing } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'
import { SCOPE_OPTIONS } from '../AssetLabeling/helpers'

import PageTitle from '../PageTitle'
import BetaBadge from '../BetaBadge'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Tabs from '../Tabs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import ModelsContent from './Content'

const Models = () => {
  const {
    query: { projectId, action, modelId },
  } = useRouter()

  const [, setPanel] = useLocalStorage({
    key: 'leftOpeningPanel',
  })

  const [, setModelFields] = useLocalStorage({
    key: `AssetLabelingAdd.${projectId}`,
    reducer: (state, a) => ({ ...state, ...a }),
    initialState: {
      modelId: modelId || '',
      label: '',
      scope: '',
    },
  })

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
            Model created.{' '}
            <Link
              href="/[projectId]/visualizer"
              as={`/${projectId}/visualizer`}
              passHref
            >
              <a
                onClick={() => {
                  setPanel({ value: 'assetLabeling' })

                  setModelFields({
                    modelId,
                    scope: SCOPE_OPTIONS[0].value,
                    label: '',
                  })
                }}
              >
                Start Labeling
              </a>
            </Link>
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
