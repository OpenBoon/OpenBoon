import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR, { mutate } from 'swr'
import Link from 'next/link'
import Router from 'next/router'

import { spacing, typography } from '../Styles'

import { fetcher } from '../Fetch/helpers'

import ButtonGroup from '../Button/Group'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import Modal from '../Modal'

const ModelDatasetHeader = ({ projectId, modelId, model, setErrors }) => {
  const [isUnlinkModalOpen, setUnlinkModalOpen] = useState(false)
  const [isUnlinking, setIsUnlinking] = useState(false)

  const {
    data: { name },
  } = useSWR(`/api/v1/projects/${projectId}/datasets/${model.datasetId}/`)

  return (
    <div css={{ display: 'flex', justifyContent: 'space-between' }}>
      <div
        css={{
          fontSize: typography.size.medium,
          lineHeight: typography.height.medium,
          fontWeight: typography.weight.medium,
        }}
      >
        {name}
      </div>

      <div css={{ marginTop: -spacing.normal }}>
        <ButtonGroup>
          <Button
            variant={BUTTON_VARIANTS.SECONDARY}
            onClick={() => {
              setErrors({})

              setIsUnlinking(false)

              setUnlinkModalOpen(true)
            }}
          >
            Unlink Dataset
          </Button>

          <Link href={`/${projectId}/datasets/${model.datasetId}`} passHref>
            <Button variant={BUTTON_VARIANTS.SECONDARY}>Go to Dataset</Button>
          </Link>
        </ButtonGroup>
      </div>

      {isUnlinkModalOpen && (
        <Modal
          title="Unlink Dataset"
          message={`Are you sure you want to unlink the "${name}" dataset?`}
          action={isUnlinking ? 'Unlinking...' : 'Unlink'}
          onCancel={() => {
            setUnlinkModalOpen(false)
          }}
          onConfirm={async () => {
            try {
              setIsUnlinking(true)

              // TODO: switch to `PATCH` and remove `name`

              await fetcher(
                `/api/v1/projects/${projectId}/models/${modelId}/`,
                {
                  method: 'PUT',
                  body: JSON.stringify({
                    datasetId: null,
                    name: model.name,
                  }),
                },
              )

              Router.push(
                `/[projectId]/models/[modelId]?action=unlink-dataset-success`,
                `/${projectId}/models/${modelId}`,
              )

              mutate(
                `/api/v1/projects/${projectId}/models/${modelId}/`,
                { ...model, datasetId: null },
                false,
              )
            } catch (response) {
              setErrors({ global: 'Something went wrong. Please try again.' })

              setIsUnlinking(false)

              setUnlinkModalOpen(false)
            }
          }}
        />
      )}
    </div>
  )
}

ModelDatasetHeader.propTypes = {
  projectId: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
  model: PropTypes.shape({
    datasetId: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
  }).isRequired,
  setErrors: PropTypes.func.isRequired,
}

export default ModelDatasetHeader
