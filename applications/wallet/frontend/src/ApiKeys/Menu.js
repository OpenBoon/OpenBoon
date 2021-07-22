import { useState } from 'react'
import PropTypes from 'prop-types'
import Router from 'next/router'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import Modal from '../Modal'

const ApiKeysMenu = ({ projectId, apiKeyId, name, revalidate }) => {
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  return (
    <>
      <Menu open="bottom-left" button={ButtonActions}>
        {({ onBlur, onClick }) => (
          <div>
            <ul>
              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={() => {
                    onClick()
                    setDeleteModalOpen(true)
                  }}
                >
                  Delete
                </Button>
              </li>
            </ul>
          </div>
        )}
      </Menu>
      {isDeleteModalOpen && (
        <Modal
          title="Delete API Key"
          message={`Are you sure you want to delete the "${name}" API key? This cannot be undone.`}
          action={isDeleting ? 'Deleting...' : 'Delete Permanently'}
          onCancel={() => {
            setDeleteModalOpen(false)
          }}
          onConfirm={async () => {
            setIsDeleting(true)

            await fetcher(
              `/api/v1/projects/${projectId}/api_keys/${apiKeyId}/`,
              { method: 'DELETE' },
            )

            await revalidate()

            Router.push(
              '/[projectId]/api-keys?action=delete-apikey-success',
              `/${projectId}/api-keys`,
            )
          }}
        />
      )}
    </>
  )
}

ApiKeysMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  apiKeyId: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default ApiKeysMenu
