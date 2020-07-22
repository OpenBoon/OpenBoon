import { useState } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'
import Link from 'next/link'

import { typography, spacing } from '../Styles'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'
import Modal from '../Modal'

import { onTrain, onDelete } from './helpers'

const LINE_HEIGHT = '23px'

const ModelDetails = () => {
  const {
    query: { projectId, modelId },
  } = useRouter()

  const [error, setError] = useState('')

  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)

  const { data: model } = useSWR(
    `/api/v1/projects/${projectId}/models/${modelId}/`,
    {
      refreshInterval: 3000,
    },
  )

  const { name, type, moduleName, ready, runningJobId } = model

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

      <ul
        css={{
          margin: 0,
          padding: 0,
          listStyle: 'none',
          fontSize: typography.size.medium,
          lineHeight: LINE_HEIGHT,
        }}
      >
        <li>
          <strong>Model Name:</strong> {name}
        </li>
        <li>
          <strong>Model Type:</strong> {type}
        </li>
        <li>
          <strong>Module Name:</strong> {moduleName}
        </li>
      </ul>

      <div>
        <ButtonGroup>
          <Button
            variant={BUTTON_VARIANTS.SECONDARY}
            onClick={() => onTrain({ apply: false, projectId, setError })}
            isDisabled={ready}
          >
            Train
          </Button>

          <Button
            type="submit"
            variant={BUTTON_VARIANTS.SECONDARY}
            onClick={() => onTrain({ apply: true, projectId, setError })}
            isDisabled={ready}
          >
            Train &amp; Apply
          </Button>

          <Button
            type="submit"
            variant={BUTTON_VARIANTS.SECONDARY}
            onClick={() => {
              setDeleteModalOpen(true)
            }}
            isDisabled={false}
          >
            Delete
          </Button>

          {isDeleteModalOpen && (
            <Modal
              title="Delete Model"
              message="Deleting this model cannot be undone."
              action="Delete Permanently"
              onCancel={() => {
                setDeleteModalOpen(false)
              }}
              onConfirm={onDelete({ setDeleteModalOpen, projectId, modelId })}
            />
          )}
        </ButtonGroup>
      </div>
    </div>
  )
}

export default ModelDetails
