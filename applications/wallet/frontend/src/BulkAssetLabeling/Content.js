import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'
import { useRouter } from 'next/router'

import { colors, constants, spacing, typography } from '../Styles'

import { useLabelTool } from '../AssetLabeling/helpers'
import { cleanup } from '../Filters/helpers'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import SuspenseBoundary from '../SuspenseBoundary'
import Form from '../Form'
import ButtonGroup from '../Button/Group'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import Modal from '../Modal'

import { onSave } from './helpers'

import BulkAssetLabelingForm from './Form'

const FROM = 0
const SIZE = 100

const BulkAssetLabelingContent = ({
  projectId,
  query,
  setIsBulkLabeling,
  datasets,
}) => {
  const {
    query: { action },
  } = useRouter()

  const q = cleanup({ query })

  const response = useSWR(
    `/api/v1/projects/${projectId}/searches/query/?query=${q}&from=${FROM}&size=${SIZE}`,
    {
      suspense: false,
      revalidateOnFocus: false,
      revalidateOnReconnect: false,
      shouldRetryOnError: false,
    },
  )

  const itemCount = response?.data?.count || -1

  const [isConfirmModalOpen, setConfirmModalOpen] = useState(false)

  const [state, dispatch] = useLabelTool({ projectId })

  const dataset = datasets.find(({ id }) => id === state.datasetId)

  const { type: datasetType = '' } = dataset || {}

  if (state.isLoading) {
    return (
      <div
        css={{
          padding: spacing.normal,
          backgroundColor: colors.structure.coal,
          borderBottom: constants.borders.regular.smoke,
        }}
      >
        <FlashMessage variant={FLASH_VARIANTS.PROCESSING}>
          Labeling {itemCount} assets with &quot;{state.lastLabel}&quot;.
        </FlashMessage>
      </div>
    )
  }

  if (action === 'bulk-labeling-success') {
    return (
      <div
        css={{
          padding: spacing.normal,
          backgroundColor: colors.structure.coal,
          borderBottom: constants.borders.regular.smoke,
        }}
      >
        <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
          {itemCount} assets labeled &quot;{state.lastLabel}&quot;.
        </FlashMessage>
      </div>
    )
  }

  return (
    <SuspenseBoundary>
      <Form style={{ width: '100%', padding: 0, overflow: 'hidden' }}>
        {state.datasetId ? (
          <BulkAssetLabelingForm
            projectId={projectId}
            datasetType={datasetType}
            state={state}
            dispatch={dispatch}
          />
        ) : (
          <div
            css={{
              padding: spacing.normal,
              color: colors.structure.white,
              fontStyle: typography.style.italic,
              borderBottom: constants.borders.regular.smoke,
            }}
          >
            Select a dataset to start labeling assets.
          </div>
        )}

        <div
          css={{
            paddingLeft: spacing.normal,
            paddingRight: spacing.normal,
          }}
        >
          <ButtonGroup>
            <Button
              variant={BUTTON_VARIANTS.SECONDARY}
              onClick={async () => {
                await dispatch({
                  datasetId: '',
                  lastLabel: '',
                  trainPct: 50,
                  labels: {},
                  isLoading: false,
                  errors: {},
                })
                setIsBulkLabeling(false)
              }}
              style={{ flex: 1 }}
              isDisabled={isConfirmModalOpen}
            >
              Cancel
            </Button>

            <Button
              type="submit"
              variant={BUTTON_VARIANTS.PRIMARY}
              onClick={() => {
                setConfirmModalOpen(true)
              }}
              isDisabled={!state.datasetId || !state.lastLabel}
              style={{ flex: 1 }}
            >
              Save
            </Button>

            {isConfirmModalOpen && (
              <Modal
                isPrimary
                title="Label All Assets in Search"
                message={`Are you sure you want to label all ${itemCount} assets in the search?`}
                action={`Label ${itemCount} Assets`}
                onCancel={() => {
                  setConfirmModalOpen(false)
                }}
                onConfirm={async () => {
                  setConfirmModalOpen(false)

                  await onSave({ projectId, query, state, dispatch })
                }}
              />
            )}
          </ButtonGroup>
        </div>
      </Form>
    </SuspenseBoundary>
  )
}

BulkAssetLabelingContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  query: PropTypes.string.isRequired,
  setIsBulkLabeling: PropTypes.func.isRequired,
  datasets: PropTypes.arrayOf(PropTypes.shape().isRequired).isRequired,
}

export default BulkAssetLabelingContent
