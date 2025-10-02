// iOS Keychain Secure Storage Implementation in Swift

import Foundation
import Security

// KeychainManager.swift
class KeychainManager {

    // MARK: - Constants
    private enum KeychainKeys {
        static let accessToken = "access_token"
        static let refreshToken = "refresh_token"
        static let userId = "user_id"
        static let userEmail = "user_email"
        static let userRoles = "user_roles"
        static let biometricEnabled = "biometric_enabled"
    }

    private let service = "com.startup.auth.keychain"

    // MARK: - Singleton
    static let shared = KeychainManager()
    private init() {}

    // MARK: - Token Management
    func storeAccessToken(_ token: String) -> Bool {
        return storeValue(token, for: KeychainKeys.accessToken)
    }

    func getAccessToken() -> String? {
        return getValue(for: KeychainKeys.accessToken)
    }

    func storeRefreshToken(_ token: String) -> Bool {
        return storeValue(token, for: KeychainKeys.refreshToken)
    }

    func getRefreshToken() -> String? {
        return getValue(for: KeychainKeys.refreshToken)
    }

    // MARK: - User Data Management
    func storeUserData(userId: Int64, email: String?, roles: [String]) -> Bool {
        var success = true

        success = success && storeValue(String(userId), for: KeychainKeys.userId)

        if let email = email {
            success = success && storeValue(email, for: KeychainKeys.userEmail)
        }

        let rolesString = roles.joined(separator: ",")
        success = success && storeValue(rolesString, for: KeychainKeys.userRoles)

        return success
    }

    func getUserId() -> Int64? {
        guard let userIdString = getValue(for: KeychainKeys.userId) else { return nil }
        return Int64(userIdString)
    }

    func getUserEmail() -> String? {
        return getValue(for: KeychainKeys.userEmail)
    }

    func getUserRoles() -> [String] {
        guard let rolesString = getValue(for: KeychainKeys.userRoles) else { return [] }
        return rolesString.split(separator: ",").map(String.init)
    }

    // MARK: - Session Management
    func isUserLoggedIn() -> Bool {
        return getAccessToken() != nil && getRefreshToken() != nil
    }

    func clearAllData() -> Bool {
        var success = true

        success = success && deleteValue(for: KeychainKeys.accessToken)
        success = success && deleteValue(for: KeychainKeys.refreshToken)
        success = success && deleteValue(for: KeychainKeys.userId)
        success = success && deleteValue(for: KeychainKeys.userEmail)
        success = success && deleteValue(for: KeychainKeys.userRoles)
        success = success && deleteValue(for: KeychainKeys.biometricEnabled)

        return success
    }

    // MARK: - Private Helper Methods
    private func storeValue(_ value: String, for key: String) -> Bool {
        let data = value.data(using: .utf8)!

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]

        // Delete existing item first
        deleteValue(for: key)

        let status = SecItemAdd(query as CFDictionary, nil)
        return status == errSecSuccess
    }

    private func getValue(for key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecReturnData as String: kCFBooleanTrue!,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        var dataTypeRef: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &dataTypeRef)

        if status == errSecSuccess,
           let data = dataTypeRef as? Data,
           let value = String(data: data, encoding: .utf8) {
            return value
        }

        return nil
    }

    private func deleteValue(for key: String) -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]

        let status = SecItemDelete(query as CFDictionary)
        return status == errSecSuccess || status == errSecItemNotFound
    }
}