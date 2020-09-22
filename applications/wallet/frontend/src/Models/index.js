/* eslint-disable jsx-a11y/click-events-have-key-events */
/* eslint-disable jsx-a11y/no-static-element-interactions */
import Head from 'next/head'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { spacing } from '../Styles'

import {
  useLocalStorageState,
  useLocalStorageReducer,
} from '../LocalStorage/helpers'

import PageTitle from '../PageTitle'
import BetaBadge from '../BetaBadge'
import FlashMessage, { VARIANTS } from '../FlashMessage'
import Tabs from '../Tabs'
import Table, { ROLES } from '../Table'

import ModelsEmpty from './Empty'
import ModelsRow from './Row'

const reducer = (state, action) => ({ ...state, ...action })

const Models = () => {
  const {
    query: { projectId, action, modelId },
  } = useRouter()

  const [, setPanel] = useLocalStorageState({
    key: 'leftOpeningPanel',
  })

  const [, localDispatch] = useLocalStorageReducer({
    key: `AssetLabelingAdd.${projectId}`,
    reducer,
  })

  return (
    <>
      <Head>
        <title>Custom Models</title>
      </Head>

      <PageTitle>
        Custom Models
        <BetaBadge />
      </PageTitle>

      {action === 'add-model-success' && (
        <div css={{ display: 'flex', paddingTop: spacing.base }}>
          <FlashMessage variant={VARIANTS.SUCCESS}>
            Model created.{' '}
            <Link
              href="/[projectId]/visualizer"
              as={`/${projectId}/visualizer`}
              passHref
            >
              <a
                onClick={() => {
                  setPanel({ value: 'assetLabeling' })
                  if (modelId) {
                    localDispatch({ modelId, label: '' })
                  }
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
          <FlashMessage variant={VARIANTS.SUCCESS}>Model deleted.</FlashMessage>
        </div>
      )}

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/models' },
          { title: 'Create New Model', href: '/[projectId]/models/add' },
        ]}
      />

      <Table
        role={ROLES.ML_Tools}
        legend="Models"
        url={`/api/v1/projects/${projectId}/models/`}
        refreshKeys={[]}
        refreshButton={false}
        columns={['Name', 'Type', 'Module']}
        expandColumn={0}
        renderEmpty={<ModelsEmpty />}
        renderRow={({ result }) => (
          <ModelsRow key={result.id} projectId={projectId} model={result} />
        )}
      />
    </>
  )
}

export default Models
