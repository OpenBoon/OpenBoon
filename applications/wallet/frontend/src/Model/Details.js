import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'
import Link from 'next/link'

import { spacing } from '../Styles'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import ItemTitle from '../Item/Title'
import ItemList from '../Item/List'
import ItemSeparator from '../Item/Separator'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import { onTrain } from './helpers'

import ModelMatrixLink from './MatrixLink'
import ModelTip from './Tip'

const ModelDetails = ({ projectId, modelId, modelTypes }) => {
  const [error, setError] = useState('')

  const { data: model } = useSWR(
    `/api/v1/projects/${projectId}/models/${modelId}/`,
    {
      refreshInterval: 3000,
    },
  )

  const {
    name,
    type,
    unappliedChanges,
    description,
    runningJobId,
    modelTypeRestrictions: { missingLabels },
  } = model

  const { label } = modelTypes.find(({ name: n }) => n === type) || {
    label: type,
  }

  return (
    <div>
      {runningJobId && (
        <div css={{ display: 'flex', paddingBottom: spacing.normal }}>
          <FlashMessage variant={FLASH_VARIANTS.PROCESSING}>
            &quot;{name}&quot; training in progress.{' '}
            <Link
              href="/[projectId]/jobs/[jobId]"
              as={`/${projectId}/jobs/${runningJobId}`}
              passHref
            >
              <a>Check Status</a>
            </Link>
          </FlashMessage>
        </div>
      )}

      {error && (
        <div css={{ display: 'flex', paddingBottom: spacing.normal }}>
          <FlashMessage variant={FLASH_VARIANTS.ERROR}>{error}</FlashMessage>
        </div>
      )}

      <ItemTitle type="Model" name={name} />

      <ItemList
        attributes={[
          ['Model Type', label],
          ['Description', description],
        ]}
      />

      <div css={{ height: spacing.normal }} />

      <ItemSeparator />

      <div css={{ height: spacing.normal }} />

      <div css={{ display: 'flex', justifyContent: 'space-between' }}>
        <div>
          <ItemList
            attributes={[
              ['Last Trained', '?'],
              ['Last Applied', '?'],
            ]}
          />

          <ButtonGroup>
            <Button
              variant={BUTTON_VARIANTS.SECONDARY}
              onClick={() =>
                onTrain({
                  model,
                  apply: false,
                  test: false,
                  projectId,
                  modelId,
                  setError,
                })
              }
              isDisabled={!unappliedChanges || !!missingLabels}
            >
              Train
            </Button>

            <ModelTip />
          </ButtonGroup>
        </div>

        <ModelMatrixLink projectId={projectId} modelId={modelId} />
      </div>
    </div>
  )
}

ModelDetails.propTypes = {
  projectId: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
  modelTypes: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
    }).isRequired,
  ).isRequired,
}

export default ModelDetails
