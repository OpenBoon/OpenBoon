import { useState } from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors } from '../Styles'

import { useLocalStorageState } from '../LocalStorage/helpers'
import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import Modal from '../Modal'

import { onDelete } from './helpers'

const AssetLabelingMenu = ({
  projectId,
  assetId,
  queryString,
  modelId,
  label,
  triggerReload,
  setError,
}) => {
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)

  const [, setLocalModelId] = useLocalStorageState({
    key: `AssetLabelingAdd.${projectId}.modelId`,
  })
  const [, setLocalLabel] = useLocalStorageState({
    key: `AssetLabelingAdd.${projectId}.label`,
  })

  return (
    <>
      <Menu
        open="left"
        button={ButtonActions}
        style={{ color: colors.structure.steel }}
      >
        {({ onClick }) => (
          <ul>
            <li>
              <Link
                href="/[projectId]/models/[modelId]"
                as={`/${projectId}/models/${modelId}`}
                passHref
              >
                <Button variant={VARIANTS.MENU_ITEM}>
                  View Model/Train & Apply
                </Button>
              </Link>
            </li>
            <li>
              <Link
                href={`/[projectId]/visualizer${queryString}`}
                as={`/${projectId}/visualizer/${queryString}`}
                passHref
              >
                <Button variant={VARIANTS.MENU_ITEM} onClick={onClick}>
                  Add Label Filter
                </Button>
              </Link>
            </li>
            <li>
              <Button
                variant={VARIANTS.MENU_ITEM}
                onClick={() => {
                  setLocalModelId({ value: modelId })
                  setLocalLabel({ value: label })

                  triggerReload()

                  onClick()
                }}
                isDisabled={false}
              >
                Edit Label
              </Button>
            </li>
            <li>
              <Button
                variant={VARIANTS.MENU_ITEM}
                onClick={() => {
                  setDeleteModalOpen(true)

                  onClick()
                }}
                isDisabled={false}
              >
                Delete Label
              </Button>
            </li>
          </ul>
        )}
      </Menu>
      {isDeleteModalOpen && (
        <Modal
          title="Delete Label"
          message="Deleting this label cannot be undone."
          action="Delete Permanently"
          onCancel={() => {
            setDeleteModalOpen(false)
          }}
          onConfirm={() => {
            setDeleteModalOpen(false)

            onDelete({
              modelId,
              label,
              projectId,
              assetId,
              setError,
            })

            // In the case where a user is deleting a Model/Label that matches
            // the current Model/Label value in the `AssetLabelingAdd` form,
            // this re-enables the submit button
            triggerReload()
          }}
        />
      )}
    </>
  )
}

AssetLabelingMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  queryString: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  triggerReload: PropTypes.func.isRequired,
  setError: PropTypes.func.isRequired,
}

export default AssetLabelingMenu
