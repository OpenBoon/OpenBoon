import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'
import Link from 'next/link'

import { spacing, constants } from '../Styles'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import ItemTitle from '../Item/Title'
import ItemList from '../Item/List'
import ItemSeparator from '../Item/Separator'
import Menu from '../Menu'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import KebabSvg from '../Icons/kebab.svg'

import ModelDeleteModal from './DeleteModal'
import ModelDetailsStates from './DetailsStates'

const ModelDetails = ({ projectId, model }) => {
  const [error, setError] = useState('')
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)

  const {
    data: { results: modelTypes },
  } = useSWR(`/api/v1/projects/${projectId}/models/model_types/`)

  const { name, type, description, runningJobId } = model

  const { label } = modelTypes.find(({ name: n }) => n === type) || {
    label: type,
  }

  return (
    <div>
      {runningJobId && (
        <div css={{ display: 'flex', paddingBottom: spacing.normal }}>
          <FlashMessage variant={FLASH_VARIANTS.PROCESSING}>
            &quot;{name}&quot; job in progress.{' '}
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

      <div>
        <div
          css={{
            display: 'flex',
            justifyContent: 'space-between',
          }}
        >
          <ItemTitle type="Model" name={name} />

          <Menu
            open="bottom-left"
            button={({ onBlur, onClick }) => (
              <Button
                aria-label="Toggle Actions Menu"
                variant={BUTTON_VARIANTS.SECONDARY}
                onBlur={onBlur}
                onClick={onClick}
                style={{
                  padding: spacing.moderate,
                  marginBottom: spacing.small,
                }}
              >
                <KebabSvg height={constants.icons.regular} />
              </Button>
            )}
          >
            {({ onBlur, onClick }) => (
              <div>
                <ul>
                  <li>
                    <Link
                      href={`/${projectId}/models/${model.id}/edit`}
                      passHref
                    >
                      <Button
                        variant={BUTTON_VARIANTS.MENU_ITEM}
                        onBlur={onBlur}
                        onClick={onClick}
                      >
                        Edit Model
                      </Button>
                    </Link>
                  </li>

                  <li>
                    <Button
                      variant={BUTTON_VARIANTS.MENU_ITEM}
                      onBlur={onBlur}
                      onClick={async () => {
                        onClick()
                        setDeleteModalOpen(true)
                      }}
                    >
                      Delete Model
                    </Button>
                  </li>
                </ul>
              </div>
            )}
          </Menu>

          <ModelDeleteModal
            projectId={projectId}
            modelId={model.id}
            name={name}
            isDeleteModalOpen={isDeleteModalOpen}
            setDeleteModalOpen={setDeleteModalOpen}
          />
        </div>

        <ItemList
          attributes={[
            ['Model Type', label],
            ['Description', description],
          ]}
        />
      </div>

      <div css={{ height: spacing.normal }} />

      <ItemSeparator />

      <ModelDetailsStates
        projectId={projectId}
        model={model}
        modelTypes={modelTypes}
        setError={setError}
      />
    </div>
  )
}

ModelDetails.propTypes = {
  projectId: PropTypes.string.isRequired,
  model: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    description: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    datasetId: PropTypes.string,
    runningJobId: PropTypes.string.isRequired,
    state: PropTypes.string.isRequired,
  }).isRequired,
}

export default ModelDetails
