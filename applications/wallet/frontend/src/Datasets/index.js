/* eslint-disable jsx-a11y/click-events-have-key-events */
/* eslint-disable jsx-a11y/no-static-element-interactions */
import Head from 'next/head'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { spacing } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'

import PageTitle from '../PageTitle'
import BetaBadge from '../BetaBadge'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Tabs from '../Tabs'
import Table, { ROLES } from '../Table'
import { usePanel, ACTIONS, MIN_WIDTH } from '../Panel/helpers'

import DatasetsRow from './Row'

const Datasets = () => {
  const {
    query: { projectId, action, datasetId },
  } = useRouter()

  const [, setPanel] = usePanel({ openToThe: 'left' })

  const [, setDatasetFields] = useLocalStorage({
    key: `AssetLabelingContent.${projectId}`,
    reducer: (state, a) => ({ ...state, ...a }),
    initialState: {
      datasetId: '',
      labels: {},
      isLoading: false,
      errors: {},
    },
  })

  return (
    <>
      <Head>
        <title>Datasets</title>
      </Head>

      <PageTitle>
        <BetaBadge isLeft />
        Datasets
      </PageTitle>

      {action === 'add-dataset-success' && (
        <div css={{ display: 'flex', paddingTop: spacing.base }}>
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            Dataset created.{' '}
            <Link
              href="/[projectId]/visualizer"
              as={`/${projectId}/visualizer`}
              passHref
            >
              <a
                onClick={() => {
                  setPanel({
                    type: ACTIONS.OPEN,
                    payload: { minSize: MIN_WIDTH, openPanel: 'assetLabeling' },
                  })

                  setDatasetFields({ datasetId, labels: {} })
                }}
              >
                Start Labeling
              </a>
            </Link>
          </FlashMessage>
        </div>
      )}

      {action === 'delete-dataset-success' && (
        <div css={{ display: 'flex', paddingTop: spacing.base }}>
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            Dataset deleted.
          </FlashMessage>
        </div>
      )}

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/datasets' },
          { title: 'Create New Dataset', href: '/[projectId]/datasets/add' },
        ]}
      />

      <Table
        role={ROLES.ML_Tools}
        legend="Datasets"
        url={`/api/v1/projects/${projectId}/datasets/`}
        refreshKeys={[]}
        refreshButton={false}
        columns={['Name', 'Type', 'Linked Models', '#Actions#']}
        expandColumn={0}
        renderEmpty="There are currently no datasets."
        renderRow={({ result, revalidate }) => (
          <DatasetsRow
            key={result.id}
            projectId={projectId}
            dataset={result}
            revalidate={revalidate}
          />
        )}
      />
    </>
  )
}

export default Datasets
